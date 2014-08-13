# HTTP

All requests must contain the `Accept: "application/vnd.lichess.v1+json"` header. Examples use [httpie](https://github.com/jakubroztocil/httpie).

## Create a game

### With A.I.

```sh
http --form POST en.l.org/setup/ai variant=1 clock=false time=60 increment=60 level=3 color=random 'Accept:application/vnd.lichess.v1+json'
```
- level: 1 to 8
- color: white | black | random
- variant: 1 (standard) | 2 (chess960) | 3 (from position) | 4 (KotH) | 5 (three-check)
- fen: if variant is 3, any valid FEN string

Response: `201 CREATED`
```javascript
{
  "game": {
    "id": "39b12Ikl",
    "variant": "chess960", // standard/chess960/fromPosition/kingOfTheHill/threeCheck
    "speed": "blitz", // bullet|blitz|classical|unlimited
    "perf": "chess960", // bullet|blitz|classical|chess960|kingOfTheHill|threeCheck
    "rated": true,
    "clock": false,
    "clockRunning": false,
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "finished": false,
    "lastMove": null,
    "moves": "",
    "player": "white",
    "started": true,
    "startedAtTurn": 0,
    "turns": 0
  },
  "clock": {
    // all durations are expressed in seconds
    "initial": 300,           // initial time of the clock, here 5 minutes
    "increment": 8,           // fisher increment
    "black": 36.0,            // current time left for black
    "white": 78.0,            // current time left for white
    "emerg": 30               // critical threshold
  },
  "player": {
    "color": "white",
    "id": "ErMy",
    "spectator": false,
    "version": 0
  },
  "opponent": {
    "color": "black",
    "ai": false,
    "user_id": "ozzie",       // request more info at /api/user/ozzie
    "isOfferingRematch": false,
    "isOfferingDraw": false,
    "isProposingTakeback": false
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

### Seek for human opponent

First you need to connect to the lobby websocket, from which you'll receive the game creation event.

```javascript
var clientId = Math.random().toString(36).substring(2); // created and stored by the client
var socketVersion = 0; // last message version number seen on this socket. Starts at zero.

var socketUrl = 'http://socket.en.l.org:9021/lobby/socket?mobile=1&sri=' + clientId + '&version=' + socketVersion;

var socket = new WebSocket(socketUrl);
```

Once connected, you can send seeks over HTTP, using the same clientId

```sh
http --form POST en.l.org/setup/hook/{clientId} variant=1 clock=false time=60 increment=60 mode=casual 'Accept:application/vnd.lichess.v1+json'
```
- clientId: same random ID created by the client and used to connect to the lobby websocket
- variant: 1 (standard) | 2 (chess960) | 3 (from position) | 4 (KotH)
- mode: casual | rated

Response: `200 OK`
```
ok
```

Now you're waiting for someone to accept the seek. The response will come as a socket message:

```javascript
// the seek was accepted
{
  "t": "redirect", // means we should move on to the game
  "id": "abcdefgh1234"
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
    "id": "39b12Ikl",
    "variant": "chess960", // standard/chess960/fromPosition/kingOfTheHill/threeCheck
    "speed": "blitz", // bullet|blitz|classical|unlimited
    "perf": "chess960", // bullet|blitz|classical|chess960|kingOfTheHill|threeCheck
    "rated": true,
    "clock": false,
    "clockRunning": false,
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "finished": false,
    "lastMove": null,
    "moves": "e4 d5 exd5 Qxd5 Nc3",
    "player": "white",
    "started": true,
    "startedAtTurn": 0,
    "turns": 5
  },
  "clock": {
    // all durations are expressed in seconds
    "initial": 300,           // initial time of the clock, here 5 minutes
    "increment": 8,           // fisher increment
    "black": 36.0,            // current time left for black
    "white": 78.0,            // current time left for white
    "emerg": 30               // critical threshold
  },
  "player": {
    "color": "white",
    "id": "ErMy",
    "spectator": false,
    "version": 0
  },
  "opponent": {
    "ai": true,
    "color": "black",
    "isOfferingRematch": false,
    "isOfferingDraw": false,
    "isProposingTakeback": false
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

## Login

Returns an authentication cookie and a `user` object.

```sh
http --form POST en.l.org/login username=thibault password=xxxxxxxx 'Accept:application/vnd.lichess.v1+json'
```

Response: `200 OK`
```
Set-Cookie: lila2="3b5cc8c80f0af258a31dc4fd1b5381cabe7388c7-sessionId=80q7V5stkKIu"; Expires=Tue, 21 Jul 2015 20:31:43 GMT; Path=/; Domain=.l.org; HTTPOnly
```
```javascript
{
  "username": "thibault",
  "title": null,                            // chess title like FM or LM (lichess master)
  "online": true,                           // is the player currently using lichess?
  "engine": false,                          // true if the user is known to use a chess engine
  "language": "en",                         // prefered language
  "profile": {
    "bio": "Developer of lichess",
    "country": "FR",
    "firstName": "Thibault",
    "lastName": "Duplessis",
    "location": "Paris"
  },
  "nowPlaying": [                           // list of games waiting for your move
    {
      "id": "abcdefgh1234",
      "opponent": {
        "id": "supercopter",
        "username": "SuperCopter",
        "rating": 2399
    },
    // more games maybe
  ],
  "perfs": {                                // user performances in different games
    "bullet": {
      "games": 35,                          // number of rated games played
      "rating": 1624,                       // Glicko2 rating
      "rd": 80                              // Glicko2 rating deviation
    },
    "chess960": {
      "games": 1,
      "rating": 1739,
      "rd": 277
    },
    "classical": {
      "games": 331,
      "rating": 1603,
      "rd": 65
    },
    "kingOfTheHill": {
      "games": 3,
      "rating": 1622,
      "rd": 223
    },
    "puzzle": {
      "games": 9,
      "rating": 1902,
      "rd": 117
    },
    "standard": {
      "games": 736,
      "rating": 1576,
      "rd": 79
    },
    "threeCheck": {
      "games": 1,
      "rating": 1662,
      "rd": 290
    }
  }
}
```

## Account info

Requires authentication.

```sh
http GET en.l.org/account/info
```

Response: `200` OK
See `user` object in `Login` section, above.

## Logout

```sh
http GET en.l.org/logout
```

Response: `200` OK

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
var clientId = Math.random().toString(36).substring(2); // created and stored by the client
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
    "v": 12,                  // game metadata has changed (could be rematch negociation, for instance)
    "t": "reloadTable",       // fetch the game document for more info
    "d": null
  }, {
    "v": 13                   // some events may come empty. Just increment the client socket version.
  }]
}
```

## Resign

```javascript
// send
{t: 'resign'}
```

## Rematch negociation

When the opponent proposes or declines a rematch,
a `reloadTable` event is sent to the client.
You should then fetch the game document to learn about
the rematch negociation state, in `opponent.isOfferingRematch`.

### Propose or accept rematch

```javascript
// send
{t: 'rematch-yes'}
```

### Decline rematch offer

```javascript
// send
{t: 'rematch-no'}
```

### Move on to the next game

When a rematch is accepted, the client receives a `redirect` event.

```javascript
// the seek was accepted
{
  "t": "redirect", // means we should move on to the game
  "id": "abcdefgh1234"
}
```

# API versioning

Current version is v1.

## Changelog

### v1

work in progress.
