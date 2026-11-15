// Package tulpor is the native VPN engine of Tulpor VPN, exposed to Android via gomobile.
//
// It runs Xray-core with its native "tun" inbound reading packets straight from
// the Android VpnService file descriptor.
package tulpor

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/xtls/xray-core/common/platform"
	xcore "github.com/xtls/xray-core/core"
	"github.com/xtls/xray-core/infra/conf/serial"
	_ "github.com/xtls/xray-core/main/distro/all"
)

var (
	mu       sync.Mutex
	instance *xcore.Instance
)

// Start launches Xray with configJSON. The config's "tun" inbound reads
// packets from tunFd (the descriptor returned by VpnService.Builder.establish).
func Start(configJSON string, tunFd int) error {
	mu.Lock()
	defer mu.Unlock()
	stopLocked()

	os.Setenv(platform.TunFdKey, strconv.Itoa(tunFd))
	inst, err := newInstance(configJSON)
	if err != nil {
		return err
	}
	if err := inst.Start(); err != nil {
		_ = inst.Close()
		return fmt.Errorf("xray start: %w", err)
	}
	instance = inst
	return nil
}

// Stop shuts down Xray. Safe to call when not running.
func Stop() {
	mu.Lock()
	defer mu.Unlock()
	stopLocked()
}

// IsRunning reports whether the engine is active.
func IsRunning() bool {
	mu.Lock()
	defer mu.Unlock()
	return instance != nil
}

func stopLocked() {
	if instance != nil {
		_ = instance.Close()
		instance = nil
	}
}

func newInstance(configJSON string) (*xcore.Instance, error) {
	cfg, err := serial.LoadJSONConfig(bytes.NewReader([]byte(configJSON)))
	if err != nil {
		return nil, fmt.Errorf("config: %w", err)
	}
	inst, err := xcore.New(cfg)
	if err != nil {
		return nil, fmt.Errorf("xray init: %w", err)
	}
	return inst, nil
}

// MeasureDelay starts a temporary Xray instance from configJSON (which must
// not contain inbounds) and returns the round-trip time in milliseconds of an
// HTTP request to testURL through the outbound. Returns -1 on failure.
func MeasureDelay(configJSON string, testURL string, timeoutMs int) int64 {
	inst, err := newInstance(configJSON)
	if err != nil {
		return -1
	}
	if err := inst.Start(); err != nil {
		return -1
	}
	defer inst.Close()

	timeout := time.Duration(timeoutMs) * time.Millisecond
	client := &http.Client{
		Timeout: timeout,
		Transport: &http.Transport{
			DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
				dest, err := parseDest(network, addr)
				if err != nil {
					return nil, err
				}
				return xcore.Dial(ctx, inst, dest)
			},
			DisableKeepAlives: false,
		},
	}
	// The first request pays for the TLS/Reality handshake; measure the second.
	var best int64 = -1
	for i := 0; i < 2; i++ {
		start := time.Now()
		resp, err := client.Get(testURL)
		if err != nil {
			continue
		}
		resp.Body.Close()
		if resp.StatusCode >= 500 {
			continue
		}
		ms := time.Since(start).Milliseconds()
		if best < 0 || ms < best {
			best = ms
		}
	}
	return best
}

// Version returns the bundled Xray-core version.
func Version() string { return xcore.Version() }

var errBadAddr = errors.New("bad address")
