#!/usr/bin/env bash
# Tulpor VPN — server setup (Xray VLESS + Reality + Vision) for Ubuntu/Debian.
#
#   curl -fsSL https://raw.githubusercontent.com/ismatillayeveldurbek-lab/VPN/main/server/install.sh | sudo bash
#
# Options (environment variables):
#   PORT=443              listening port
#   SNI=www.microsoft.com site Reality imitates (must support TLS 1.3 + HTTP/2)
#   NAME="DE-Frankfurt"   server name shown in the app (default: <country>-<city> from IP)
#
# Commands:  install.sh            install / reinstall
#            install.sh link       print the vless:// link again
#            install.sh add-user   add another user (device) and print its link
set -euo pipefail

PORT="${PORT:-443}"
SNI="${SNI:-www.microsoft.com}"
CONF_DIR="${CONF_DIR:-/usr/local/etc/xray}"
STATE="$CONF_DIR/tulpor.env"
XRAY="${XRAY:-/usr/local/bin/xray}"
DRY_RUN="${DRY_RUN:-0}"   # 1 = only write the config (used by tests)

red() { printf '\033[31m%s\033[0m\n' "$*"; }
gold() { printf '\033[33m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }

need_root() { [ "$DRY_RUN" = 1 ] || [ "$(id -u)" = 0 ] || { red "Root huquqi kerak: sudo bilan ishga tushiring"; exit 1; }; }

install_packages() {
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq curl unzip openssl qrencode ca-certificates >/dev/null
}

install_xray() {
  gold "Xray o'rnatilmoqda..."
  bash -c "$(curl -fsSL https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install >/dev/null
}

enable_bbr() {
  if ! sysctl net.ipv4.tcp_congestion_control 2>/dev/null | grep -q bbr; then
    printf 'net.core.default_qdisc=fq\nnet.ipv4.tcp_congestion_control=bbr\n' > /etc/sysctl.d/99-tulpor-bbr.conf
    sysctl --system >/dev/null 2>&1 || true
  fi
}

open_firewall() {
  if command -v ufw >/dev/null && ufw status | grep -q "Status: active"; then ufw allow "$PORT"/tcp >/dev/null; fi
}

gen_keys() {
  # Xray prints "Private key:/Public key:" (old) or "PrivateKey:/Password:" (new).
  local out; out="$("$XRAY" x25519)"
  PRIVATE_KEY="$(echo "$out" | awk -F': *' 'tolower($1) ~ /private ?key/ {print $2; exit}')"
  PUBLIC_KEY="$(echo "$out" | awk -F': *' 'tolower($1) ~ /public ?key|password/ {print $2; exit}')"
  [ -n "$PRIVATE_KEY" ] && [ -n "$PUBLIC_KEY" ] || { red "x25519 kalit yaratilmadi"; exit 1; }
}

server_ip() { curl -fsS4 --max-time 8 https://api.ipify.org || curl -fsS4 --max-time 8 https://ifconfig.me; }

default_name() {
  local cc city
  cc="$(curl -fsS --max-time 8 https://ipinfo.io/country 2>/dev/null | tr -d '[:space:]' || true)"
  city="$(curl -fsS --max-time 8 https://ipinfo.io/city 2>/dev/null | tr -d '\r\n' || true)"
  if [ -n "$cc" ]; then echo "${cc}-${city:-Server}"; else echo "Tulpor"; fi
}

clients_json() {
  local first=1
  for id in $UUIDS; do
    [ $first = 1 ] || printf ','
    printf '{"id":"%s","flow":"xtls-rprx-vision"}' "$id"; first=0
  done
}

write_config() {
  mkdir -p "$CONF_DIR"
  cat > "$CONF_DIR/config.json" <<JSON
{
  "log": {"loglevel": "warning"},
  "inbounds": [{
    "listen": "0.0.0.0",
    "port": $PORT,
    "protocol": "vless",
    "settings": {"clients": [$(clients_json)], "decryption": "none"},
    "streamSettings": {
      "network": "tcp",
      "security": "reality",
      "realitySettings": {
        "dest": "$SNI:443",
        "serverNames": ["$SNI"],
        "privateKey": "$PRIVATE_KEY",
        "shortIds": ["$SHORT_ID"]
      }
    },
    "sniffing": {"enabled": true, "destOverride": ["http", "tls", "quic"], "routeOnly": true}
  }],
  "outbounds": [
    {"tag": "direct", "protocol": "freedom"},
    {"tag": "block", "protocol": "blackhole"}
  ],
  "routing": {"rules": [
    {"type": "field", "ip": ["127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16", "100.64.0.0/10", "fc00::/7", "fe80::/10"], "outboundTag": "block"},
    {"type": "field", "protocol": ["bittorrent"], "outboundTag": "block"}
  ]}
}
JSON
}

save_state() {
  umask 077
  cat > "$STATE" <<ENV
PORT="$PORT"
SNI="$SNI"
NAME="$NAME"
SERVER_IP="$SERVER_IP"
UUIDS="$UUIDS"
PRIVATE_KEY="$PRIVATE_KEY"
PUBLIC_KEY="$PUBLIC_KEY"
SHORT_ID="$SHORT_ID"
ENV
}

link_for() {
  local id="$1" name="$2"
  printf 'vless://%s@%s:%s?encryption=none&flow=xtls-rprx-vision&security=reality&sni=%s&fp=chrome&pbk=%s&sid=%s&type=tcp#%s\n' \
    "$id" "$SERVER_IP" "$PORT" "$SNI" "$PUBLIC_KEY" "$SHORT_ID" "$name"
}

show_link() {
  local link="$1"
  echo
  green "✅ Tayyor! Ushbu havolani Tulpor ilovasiga qo'shing (Serverlar → +):"
  echo
  gold "$link"
  echo
  if command -v qrencode >/dev/null; then qrencode -t ANSIUTF8 "$link"; fi
}

restart_xray() { systemctl enable xray >/dev/null 2>&1 || true; systemctl restart xray; sleep 1; systemctl is-active --quiet xray || { red "Xray ishga tushmadi: journalctl -u xray"; exit 1; }; }

cmd_install() {
  need_root
  if [ "$DRY_RUN" != 1 ]; then install_packages; install_xray; enable_bbr; open_firewall; fi
  gen_keys
  SHORT_ID="$(openssl rand -hex 8)"
  UUIDS="$("$XRAY" uuid)"
  SERVER_IP="${SERVER_IP:-$(server_ip)}"
  NAME="${NAME:-$(default_name)}"
  write_config
  "$XRAY" run -test -c "$CONF_DIR/config.json" >/dev/null || { red "Konfiguratsiya xato"; exit 1; }
  save_state
  [ "$DRY_RUN" = 1 ] || restart_xray
  show_link "$(link_for "${UUIDS%% *}" "$NAME")"
}

load_state() { [ -f "$STATE" ] || { red "Avval o'rnating: install.sh"; exit 1; }; . "$STATE"; }

cmd_link() {
  load_state
  local i=0
  for id in $UUIDS; do
    i=$((i + 1)); link_for "$id" "$NAME$([ $i -gt 1 ] && echo "-$i")"
  done
}

cmd_add_user() {
  need_root; load_state
  local id; id="$("$XRAY" uuid)"
  UUIDS="$UUIDS $id"
  write_config
  "$XRAY" run -test -c "$CONF_DIR/config.json" >/dev/null
  save_state
  [ "$DRY_RUN" = 1 ] || restart_xray
  show_link "$(link_for "$id" "$NAME-$(echo "$UUIDS" | wc -w)")"
}

case "${1:-install}" in
  install) cmd_install ;;
  link) cmd_link ;;
  add-user) cmd_add_user ;;
  *) echo "Usage: $0 [install|link|add-user]"; exit 1 ;;
esac
