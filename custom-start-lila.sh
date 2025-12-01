#!/usr/bin/env bash
set -euo pipefail

LILA_DIR="${LILA_DIR:-"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"}"

# Prefer env var, else try sibling ../lila-ws, else fall back to old path.
if [[ -n "${LILA_WS_DIR:-}" ]]; then
  : # keep it
elif [[ -d "$LILA_DIR/../lila-ws" ]]; then
  LILA_WS_DIR="$(cd "$LILA_DIR/../lila-ws" && pwd)"
else
  LILA_WS_DIR="$HOME/lichess/lila-ws"
fi

CSRF_ORIGIN="${LILA_CSRF_ORIGIN:-http://localhost:9663}"

tmux new-session -d -s my_session "cd '$LILA_DIR' && source ~/.zshrc && echo 'Command: ./lila.sh'; bash"
tmux setw remain-on-exit on
tmux split-window -h "cd '$LILA_DIR' && ui/build -w; bash"
tmux split-window -v "cd '$LILA_DIR' && killall redis-server && sleep 1; redis-server; bash"
tmux select-pane -t 0
tmux split-window -v "cd '$LILA_DIR' && mongod; bash"
tmux split-window -h "cd '$LILA_WS_DIR' && source ~/.zshrc && sbt run -Dcsrf.origin=$CSRF_ORIGIN; bash"
tmux swap-pane -s 1 -t 2
tmux -2 attach-session -d
