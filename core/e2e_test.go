package tulpor

import (
	"crypto/ecdh"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
)

func freePort(t *testing.T) int {
	l, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer l.Close()
	return l.Addr().(*net.TCPAddr).Port
}

// Spins up a real VLESS + Reality + Vision server and measures a request
// through it with the same outbound shape the Android app generates.
func TestRealityEndToEnd(t *testing.T) {
	// Reality "camouflage" target: any TLS 1.3 server.
	dest := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
	dest.EnableHTTP2 = true
	dest.StartTLS()
	defer dest.Close()

	// Site the client actually wants to reach through the tunnel.
	site := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	}))
	defer site.Close()

	priv, err := ecdh.X25519().GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	enc := base64.RawURLEncoding
	privKey, pubKey := enc.EncodeToString(priv.Bytes()), enc.EncodeToString(priv.PublicKey().Bytes())
	uuid := "b831381d-6324-4d53-ad4f-8cda48b30811"
	port := freePort(t)

	server := fmt.Sprintf(`{"log":{"loglevel":"none"},
	"inbounds":[{"listen":"127.0.0.1","port":%d,"protocol":"vless",
	  "settings":{"clients":[{"id":"%s","flow":"xtls-rprx-vision"}],"decryption":"none"},
	  "streamSettings":{"network":"tcp","security":"reality","realitySettings":{
	    "dest":"%s","serverNames":["example.com"],"privateKey":"%s","shortIds":["6ba85179e30d4fc2"]}}}],
	"outbounds":[{"protocol":"freedom"}]}`, port, uuid, dest.Listener.Addr().String(), privKey)
	srv, err := newInstance(server)
	if err != nil {
		t.Fatalf("server config: %v", err)
	}
	if err := srv.Start(); err != nil {
		t.Fatal(err)
	}
	defer srv.Close()

	client := fmt.Sprintf(`{"log":{"loglevel":"none"},"outbounds":[{"tag":"proxy","protocol":"vless",
	 "settings":{"vnext":[{"address":"127.0.0.1","port":%d,
	   "users":[{"id":"%s","encryption":"none","flow":"xtls-rprx-vision"}]}]},
	 "streamSettings":{"network":"tcp","security":"reality","realitySettings":{
	   "serverName":"example.com","fingerprint":"chrome","publicKey":"%s","shortId":"6ba85179e30d4fc2","spiderX":"/"}}}]}`,
		port, uuid, pubKey)

	if d := MeasureDelay(client, site.URL, 5000); d < 0 {
		t.Fatal("request through VLESS+Reality tunnel failed")
	} else {
		t.Logf("tunnel delay: %d ms", d)
	}

	// Wrong key must not get through.
	bad, _ := ecdh.X25519().GenerateKey(rand.Reader)
	badClient := fmt.Sprintf(`{"log":{"loglevel":"none"},"outbounds":[{"protocol":"vless",
	 "settings":{"vnext":[{"address":"127.0.0.1","port":%d,"users":[{"id":"%s","encryption":"none","flow":"xtls-rprx-vision"}]}]},
	 "streamSettings":{"network":"tcp","security":"reality","realitySettings":{
	   "serverName":"example.com","fingerprint":"chrome","publicKey":"%s","shortId":"6ba85179e30d4fc2"}}}]}`,
		port, uuid, enc.EncodeToString(bad.PublicKey().Bytes()))
	if d := MeasureDelay(badClient, site.URL, 3000); d >= 0 {
		t.Fatalf("expected failure with wrong public key, got %d ms", d)
	}
}
