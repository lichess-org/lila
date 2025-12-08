#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP="$ROOT/.tmp"
mkdir -p "$TMP"

CADDY_LOG="$TMP/caddy.log"
CLOUDFLARE_LOG="$TMP/cloudflared.log"
LOCAL_CONF="$ROOT/conf/local.conf"
APP_CONF="$ROOT/conf/application.conf"
APP_CONF_DEFAULT="$ROOT/conf/application.conf.default"

escape_applescript_string() {
  local s="$1"
  s="${s//\\/\\\\}"   # \  -> \\
  s="${s//\"/\\\"}"   # "  -> \"
  s="${s//$'\n'/ }"   # newlines -> spaces
  printf '%s' "$s"
}

open_terminal_tab() {
  local cmd="$1"
  local esc
  esc="$(escape_applescript_string "$cmd")"

  /usr/bin/osascript <<APPLESCRIPT
tell application "Terminal"
  activate
  if not (exists front window) then
    do script ""
  end if
end tell

tell application "System Events"
  tell process "Terminal"
    keystroke "t" using command down
    delay 0.2
  end tell
end tell

tell application "Terminal"
  do script "$esc" in selected tab of front window
end tell
APPLESCRIPT
}

ensure_application_includes_local() {
  if [[ ! -f "$APP_CONF" ]]; then
    cp "$APP_CONF_DEFAULT" "$APP_CONF"
  fi
  if ! grep -qE '^\s*include\s+"local"\s*$' "$APP_CONF"; then
    printf '\ninclude "local"\n' >> "$APP_CONF"
  fi
}

write_local_conf() {
  local url="$1"
  local domain="${url#https://}"
  cat > "$LOCAL_CONF" <<EOF
net {
  domain = "$domain"
  socket.domains = ["$domain"]
  socket.alts = []
  base_url = "$url"
}
EOF
}

# (Recommended) pre-clean so you don't stack old processes forever:
pkill -f "cloudflared tunnel.*--url http://127\.0\.0\.1:9670" 2>/dev/null || true
pkill -f "cloudflared tunnel.*--url http://localhost:9670" 2>/dev/null || true
pkill -f "caddy run --config $ROOT/Caddyfile" 2>/dev/null || true

ensure_application_includes_local

: > "$CADDY_LOG"
: > "$CLOUDFLARE_LOG"

open_terminal_tab "cd \"$ROOT\" && echo \"[caddy]\" && caddy run --config \"$ROOT/Caddyfile\" 2>&1 | tee -a \"$CADDY_LOG\""
open_terminal_tab "cd \"$ROOT\" && echo \"[cloudflared]\" && cloudflared tunnel --protocol http2 --edge-ip-version 4 --url http://127.0.0.1:9670 --no-autoupdate 2>&1 | tee -a \"$CLOUDFLARE_LOG\""

# Auto-detect the trycloudflare URL from the log, write conf/local.conf
PUBLIC_URL=""
for _ in $(seq 1 300); do
  PUBLIC_URL="$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$CLOUDFLARE_LOG" | tail -n 1 || true)"
  [[ -n "$PUBLIC_URL" ]] && break
  sleep 0.1
done

if [[ -z "$PUBLIC_URL" ]]; then
  echo "Could not detect trycloudflare URL. Check: $CLOUDFLARE_LOG" >&2
else
  echo "Public URL: $PUBLIC_URL"
  write_local_conf "$PUBLIC_URL"
  export LILA_CSRF_ORIGIN="$PUBLIC_URL"
fi

exec "$ROOT/auxiliary-script.sh"
