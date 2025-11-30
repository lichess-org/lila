#!/bin/bash
LILA_DIR="/Users/john.doknjas/lichess/lila"
LILA_WS_DIR="/Users/john.doknjas/lichess/lila-ws"
tmux new-session -d -s my_session "cd '$LILA_DIR' && source ~/.zshrc && echo 'Command: ./lila.sh'; bash"
tmux setw remain-on-exit on
tmux split-window -h "cd '$LILA_DIR' && ui/build -w; bash"
tmux split-window -v "cd '$LILA_DIR' && killall redis-server && sleep 1; redis-server; bash"
tmux select-pane -t 0
tmux split-window -v "cd '$LILA_DIR' && mongod; bash"
tmux split-window -h "cd '$LILA_WS_DIR' && source ~/.zshrc && sbt run -Dcsrf.origin=http://localhost:9663; bash"
tmux swap-pane -s 1 -t 2
tmux -2 attach-session -d