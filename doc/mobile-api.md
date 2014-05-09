# HTTP

All requests must contain the `Accept: "application/vnd.lichess.v1+json"` header.
Examples use [httpie](https://github.com/jakubroztocil/httpie).
The output is not documented. Instead, run the command example.

## Create a game

```sh
http --form POST en.l.org/setup/ai variant=1 clock=false time=60 increment=60 level=3 color=random 'Accept:application/vnd.lichess.v1+json'
```
- variant: 1 (standard) or 2 (chess960)
- level: 1 to 8

# WEBSOCKET

// connect
var socket = new WebSocket("ws://en.lichess.org/api/socket");

// send a message
socket.send(JSON.stringify({act: 'create', type: 'ai', level: 3}));

// receive messages
socket.onmessage = function(e) {
  var msg = JSON.parse(e.data);
  if (msg.act == 'start') console.debug(msg.game);
};

CLIENT -> SERVER

