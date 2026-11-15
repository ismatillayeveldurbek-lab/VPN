package tulpor

import (
	"net"
	"strconv"

	xnet "github.com/xtls/xray-core/common/net"
)

func parseDest(network, addr string) (xnet.Destination, error) {
	host, portStr, err := net.SplitHostPort(addr)
	if err != nil {
		return xnet.Destination{}, err
	}
	port, err := strconv.Atoi(portStr)
	if err != nil {
		return xnet.Destination{}, errBadAddr
	}
	if network == "udp" {
		return xnet.UDPDestination(xnet.ParseAddress(host), xnet.Port(port)), nil
	}
	return xnet.TCPDestination(xnet.ParseAddress(host), xnet.Port(port)), nil
}
