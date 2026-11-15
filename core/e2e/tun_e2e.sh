#!/usr/bin/env bash
# Full-path VPN test on Linux, mirroring the phone:
#   "phone" netns --(all traffic)--> TUN --> Xray client (app-generated config)
#     --VLESS+Reality+Vision--> Xray server (config from server/install.sh) --> target site
# Needs root, /dev/net/tun, iproute2, python3, an xray binary ($XRAY) and the app config
# exported by ConfigBuilderTest (app/build/generated-vpn-config.json).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
XRAY="${XRAY:?path to xray binary}"
W="$(mktemp -d)"
NS=tulpor-phone
SITE_IP=198.51.100.1       # public-looking, so the app's "private IP -> direct" rule does not apply
DNS_IP=198.51.100.53       # stands in for the public resolver the app uses
SRV_IP=10.200.0.1
PHONE_IP=10.200.0.2
PIDS=()

cleanup() {
  for p in "${PIDS[@]:-}"; do kill "$p" 2>/dev/null || true; done
  # background helpers started with `python3 -c` / openssl may outlive their shell job
  pkill -f "TCPServer\(\('$SITE_IP'" 2>/dev/null || true
  pkill -f "bind\(\('$DNS_IP'" 2>/dev/null || true
  pkill -f "s_server -accept 127.0.0.1:9443" 2>/dev/null || true
  ip netns del $NS 2>/dev/null || true
  ip link del veth-srv 2>/dev/null || true
  ip addr del $SITE_IP/32 dev lo 2>/dev/null || true
  ip addr del $DNS_IP/32 dev lo 2>/dev/null || true
  rm -rf /etc/netns/$NS
  rm -rf "$W"
}
trap cleanup EXIT

# --- network: phone namespace connected to the "internet" (root ns) by a veth pair
ip netns add $NS
ip link add veth-srv type veth peer name veth-phone
ip link set veth-phone netns $NS
ip addr add $SRV_IP/24 dev veth-srv && ip link set veth-srv up
ip netns exec $NS ip addr add $PHONE_IP/24 dev veth-phone
ip netns exec $NS ip link set veth-phone up
ip netns exec $NS ip link set lo up
ip addr add $SITE_IP/32 dev lo
ip addr add $DNS_IP/32 dev lo

# --- target site and Reality camouflage target (any TLS 1.3 server) on the "internet"
python3 -c "
import http.server, socketserver
class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        b=b'hello-from-site'; self.send_response(200); self.send_header('Content-Length',str(len(b))); self.end_headers(); self.wfile.write(b)
    def log_message(self,*a): pass
socketserver.TCPServer.allow_reuse_address=True
socketserver.TCPServer(('$SITE_IP',8080),H).serve_forever()" &
PIDS+=($!)
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes -days 1 -subj /CN=example.com \
  -keyout "$W/k.pem" -out "$W/c.pem" 2>/dev/null
openssl s_server -accept 127.0.0.1:9443 -tls1_3 -key "$W/k.pem" -cert "$W/c.pem" -quiet -www >/dev/null 2>&1 &
PIDS+=($!)

# --- tiny DNS server on the "internet": answers every A query with the site's IP
python3 -c "
import socket, struct
s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.bind(('$DNS_IP',53))
ip=bytes(int(x) for x in '$SITE_IP'.split('.'))
while True:
    q,a=s.recvfrom(512)
    end=12
    while q[end]: end+=q[end]+1
    end+=5
    qtype=struct.unpack('>H', q[end-4:end-2])[0]
    ans=(b'\xc0\x0c'+struct.pack('>HHIH',1,1,60,4)+ip) if qtype==1 else b''
    hdr=q[:2]+b'\x81\x80'+q[4:6]+struct.pack('>HHH',1 if ans else 0,0,0)
    s.sendto(hdr+q[12:end]+ans,a)" &
PIDS+=($!)

# --- server: config produced by server/install.sh (dry run), Reality dest -> local TLS server
mkdir -p "$W/srv"
DRY_RUN=1 XRAY="$XRAY" CONF_DIR="$W/srv" SERVER_IP=$SRV_IP PORT=4433 SNI=example.com NAME=DE-Test \
  bash "$ROOT/server/install.sh" > "$W/install.out" 2>&1
LINK="$(DRY_RUN=1 XRAY="$XRAY" CONF_DIR="$W/srv" bash "$ROOT/server/install.sh" link | head -1)"
python3 - "$W/srv/config.json" <<'PY'
import json,sys; p=sys.argv[1]; c=json.load(open(p))
c["inbounds"][0]["streamSettings"]["realitySettings"]["dest"]="127.0.0.1:9443"
# the test site lives on a "private" host here; allow it for the test only
c["routing"]["rules"]=[r for r in c["routing"]["rules"] if "ip" not in r]
json.dump(c,open(p,"w"))
PY
"$XRAY" run -c "$W/srv/config.json" > "$W/server.log" 2>&1 &
PIDS+=($!)

# --- client: the exact JSON the Android app generates, pointed at this server's link
python3 - "$ROOT/app/build/generated-vpn-config.json" "$LINK" "$W/client.json" <<'PY'
import json,sys,urllib.parse as u
cfg=json.load(open(sys.argv[1])); link=sys.argv[2]
p=u.urlparse(link); q=dict(u.parse_qsl(p.query))
out=cfg["outbounds"][0]; v=out["settings"]["vnext"][0]
v["address"]=p.hostname; v["port"]=p.port; v["users"][0]["id"]=p.username
r=out["streamSettings"]["realitySettings"]
r["serverName"]=q["sni"]; r["publicKey"]=q["pbk"]; r["shortId"]=q["sid"]
cfg["log"]={"loglevel":"warning"}
# the app's resolvers are public; here the "public" resolver is the local test DNS
cfg["dns"]["servers"]=["198.51.100.53"]
json.dump(cfg,open(sys.argv[3],"w"),indent=1)
PY
ip netns exec $NS "$XRAY" run -c "$W/client.json" > "$W/client.log" 2>&1 &
PIDS+=($!)

# --- phone routing, like VpnService: everything into the TUN, except the tunnel itself
for i in $(seq 1 50); do ip netns exec $NS ip link show tulpor0 >/dev/null 2>&1 && break; sleep 0.1; done
ip netns exec $NS ip addr add 172.19.0.1/30 dev tulpor0
ip netns exec $NS ip link set tulpor0 up
ip netns exec $NS ip route add default dev tulpor0
# like VpnService.addDnsServer(): the phone sends DNS to a resolver address inside the tunnel
mkdir -p /etc/netns/$NS && echo "nameserver 1.1.1.1" > /etc/netns/$NS/resolv.conf
sleep 1

echo "link: ${LINK%%\?*}..."
if body="$(ip netns exec $NS curl -sS --max-time 10 http://$SITE_IP:8080/)" && [ "$body" = hello-from-site ]; then
  echo "PASS: phone -> TUN -> VLESS+Reality -> site returned '$body'"
else
  echo "FAIL: got '${body:-}'"; echo "--- client log"; tail -20 "$W/client.log"; echo "--- server log"; tail -20 "$W/server.log"; exit 1
fi

# DNS: the phone asks 1.1.1.1 (through the TUN); Xray's dns-out answers via the tunnel.
if body="$(ip netns exec $NS curl -sS --max-time 10 http://site.example:8080/)" && [ "$body" = hello-from-site ]; then
  echo "PASS: DNS through the tunnel resolved site.example and the page loaded"
else
  echo "FAIL: DNS through tunnel, got '${body:-}'"; tail -20 "$W/client.log"; exit 1
fi

# Without the tunnel the site is unreachable from the phone (proves traffic really went through Xray).
ip netns exec $NS ip route del default dev tulpor0
if ip netns exec $NS curl -s --max-time 3 http://$SITE_IP:8080/ >/dev/null; then
  echo "FAIL: site reachable without tunnel, test is not meaningful"; exit 1
fi
echo "PASS: site unreachable without the tunnel"
