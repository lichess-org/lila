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

## Unique `clientId`

The client is responsible for creating and storing its own unique `clientId`.
It will be sent to the server when connecting to a websocket.

Suggestion of implementation:
```javascript
var clientId = Math.random().toString(36).substring(2);
```

## Connect to a game as a player

```javascript
var playerId; // obtained from game creation API
var clientId; // created by the client
var socketVersion = 0; // last message version number seen on this socket. Starts at zero.

var socketUrl = 'http://socket.en.l.org:9021/' + playerId + '/socket?sri=' + clientId + '&version=' + socketVersion;

var socket = new WebSocket(socketUrl);
```

## Ping

The client should ping the server every second.

```javascript
socket.send(JSON.stringify({t: 'p', v: socketVersion}));
```
