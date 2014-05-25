# HTTP

All requests must contain the `Accept: "application/vnd.lichess.v1+json"` header. Examples use [httpie](https://github.com/jakubroztocil/httpie).

## Create a game

```sh
http --form POST en.l.org/setup/ai variant=1 clock=false time=60 increment=60 level=3 color=random 'Accept:application/vnd.lichess.v1+json'
```
- level: 1 to 8
- color: white|black|random
- variant: 1 (standard) | 2 (chess960) | 3 (from position)
- fen: if variant is 3, any valid FEN string

Response: `201 CREATED`
```javascript
{
  "game": {
    "clock": false,
    "clockRunning": false,
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "finished": false,
    "id": "39b12Ikl",
    "lastMove": null,
    "moves": "",
    "player": "white",
    "started": true,
    "startedAtTurn": 0,
    "turns": 0
  },
  "clock": {
    // all durations are expressed in seconds
    "black": 3600.0,
    "emerg": 30,              // critical threshold
    "white": 3600.0
  },
  "player": {
    "color": "white",
    "id": "ErMy",
    "spectator": false,
    "version": 0
  },
  "opponent": {
    "ai": true,
    "color": "black"
  },
  "possibleMoves": {          // list of moves you can play. Empty if not your turn to play.
    "a2": "a3a4",             // from a2, you can go on a3 or a4.
    "b1": "a3c3",
    "b2": "b3b4",
    "c2": "c3c4",
    "d2": "d3d4",
    "e2": "e3e4",
    "f2": "f3f4",
    "g1": "f3h3",
    "g2": "g3g4",
    "h2": "h3h4"
  },
  "pref": {
    "animationDelay": 240,
    "autoQueen": 2,
    "autoThreefold": 2,
    "clockBar": true,
    "clockTenths": true,
    "enablePremove": true
  },
  "url": {
    "pov": "/39b12IklErMy",
    "end": "/39b12IklErMy/end",
    "socket": "/39b12IklErMy/socket/v1",
    "table": "/39b12IklErMy/table"
  },
  "tournamentId": null
}
```

## Fetch a game as a player (POV)

```sh
http GET en.l.org/39b12IklErMy 'Accept:application/vnd.lichess.v1+json'
```

Response: `200 OK`
```javascript
{
  "game": {
    "clock": false,
    "clockRunning": false,
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "finished": false,
    "id": "39b12Ikl",
    "lastMove": null,
    "moves": "e4 d5 exd5 Qxd5 Nc3",
    "player": "white",
    "started": true,
    "startedAtTurn": 0,
    "turns": 5
  },
  "clock": {
    // all durations are expressed in seconds
    "black": 3600.0,
    "emerg": 30,              // critical threshold
    "white": 3600.0
  },
  "player": {
    "color": "white",
    "id": "ErMy",
    "spectator": false,
    "version": 0
  },
  "opponent": {
    "ai": true,
    "color": "black"
  },
  "possibleMoves": {          // list of moves you can play. Empty if not your turn to play.
    "a2": "a3a4",             // from a2, you can go on a3 or a4.
    "b1": "a3c3",
    "b2": "b3b4",
    "c2": "c3c4",
    "d2": "d3d4",
    "e2": "e3e4",
    "f2": "f3f4",
    "g1": "f3h3",
    "g2": "g3g4",
    "h2": "h3h4"
  },
  "pref": {
    "animationDelay": 240,
    "autoQueen": 2,
    "autoThreefold": 2,
    "clockBar": true,
    "clockTenths": true,
    "enablePremove": true
  },
  "url": {
    "pov": "/39b12IklErMy",
    "end": "/39b12IklErMy/end",
    "socket": "/39b12IklErMy/socket/v1",
    "table": "/39b12IklErMy/table"
  },
  "tournamentId": null
}
```

## Fetch informations about finished game

When the `end` event is received on the socket,
you may call the `end` URL provided by the API
to retrieve informations about the result of the game.

```sh
http GET en.l.org/39b12IklErMy/end 'Accept:application/vnd.lichess.v1+json'
```

Response: `200 OK`
```javascript
{
    "isEnd": true,
    "status": {
        "id": 31,
        "name": "Resign",
        "translated": "Black resigned"
    },
    "winner": {
        "isMe": false,
        "name": "Matador_Angelo (1864)",
        "userId": "matador_angelo"
    }
}
```

# WEBSOCKET

## Unique `clientId`

The client is responsible for creating and storing its own unique `clientId`.
It will be sent to the server when connecting to a websocket.

Suggestion of implementation:
```javascript
var clientId = Math.random().toString(36).substring(2);
```

## Message format

All websocket messages, sent or received, are composed of a type `t` and data `d`. Example:

```javascript
{t: 'move', d: {from: 'e2', to: 'e4'}}
```

## Connect to a game as a player

```javascript
var baseUrl; // obtained from game creation API (`url.socket`)
var clientId; // created by the client
var socketVersion = 0; // last message version number seen on this socket. Starts at zero.

var socketUrl = 'http://socket.en.l.org:9021' + baseUrl + '?sri=' + clientId + '&version=' + socketVersion;

var socket = new WebSocket(socketUrl);
```

## Ping

The client should ping the server every second.

```javascript
// send
{t: 'p', v: socketVersion}
```

## Pong

The server answers client pings with a message of type `n`, containing the number of online players.

```javascript
// receive
{t: 'n', d: 1570}
```

The delay between `ping` and `pong` can be used to calculate the client lag.

## Send a move

```javascript
// send
{t: 'move', d: {from: 'e2', to: 'e4'}}
```

To promote, specify an additional `promotion` key.
Accepted values are `queen`, `knight`, `rook`, or `bishop`.

```javascript
// send
{t: 'move', d: {from: 'e7', to: 'e8', promotion: 'knight'}}
```

## Receive game status

The message data `d` is an array of events.
Each event has a version number `v`, a type `t` and data `d`.

```javascript
// receive
{
  "t": "b",                   // "b" is the batch type.
  "d": [{
    "v": 8,                   // message version
    "t": "possibleMoves",     // list of moves you can play. Empty if not your turn to play.
    "d": {
      "b2": "b3b4",           // from b2, you can either go on b3 or b4.
      "e1": "e2",
      "g2": "g3g4",
      "f1": "e2d3c4b5a6",
      "c2": "c3c4",
      "b1": "a3c3",
      "g1": "f3h3e2",
      "e4": "e5",
      "h2": "h3h4",
      "d2": "d3d4",
      "d1": "e2f3g4h5",
      "a2": "a3a4",
      "f2": "f3f4"
    }
  }, {
    "v": 9,
    "t": "state",             // data about the game status
    "d": {
      "color": "white",
      "turns": 2
    }
  }, {
    "v": 10,
    "t": "move",
    "d": {
      "type": "move",         // can be a move you played (server confirmation) or your opponent move.
      "from": "e7",
      "to": "e6",
      "color": "black"
    }
  }, {
    "v": 11,                  // if the user has set a premove, now is the time to play it.
    "t": "premove",
    "d": null
  }, {
    "v": 12,                  // ignore that for now.
    "t": "reloadTable",
    "d": null
  }, {
    "v": 13                   // some events may come empty. Just increment the client socket version.
  }]
}
```

# API versioning

Current version is v1.

## Changelog

### v1

work in progress.
