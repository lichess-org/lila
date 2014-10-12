# Play a game

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
    "running": true,
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
    "version": 0,
    "isOfferingRematch": true,  // field is missing when false
    "isOfferingDraw": true,     // field is missing when false
    "isProposingTakeback": true // field is missing when false
  },
  "opponent": {
    "ai": true,
    "color": "black",
    "isOfferingRematch": true,  // field is missing when false
    "isOfferingDraw": true,     // field is missing when false
    "isProposingTakeback": true // field is missing when false
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
  "chat": {
    "lines": [
      {
        "u": "legend",
        "t": "Hi there, I'm logged in, my name is legend"
      },
      {
        "c": "black",
        "t": "Hello! I'm anonymous, playing with the black pieces"
      }
    ]
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

## Connect to a game as a player

```javascript
var baseUrl; // obtained from game creation API (`url.socket`)
var clientId = Math.random().toString(36).substring(2); // created and stored by the client
var socketVersion = 0; // last message version number seen on this socket. Starts at zero.

var socketUrl = 'http://socket.en.l.org:9021' + baseUrl + '?sri=' + clientId + '&version=' + socketVersion;

var socket = new WebSocket(socketUrl);
```

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
    "v": 12,                  // game metadata has changed (could be rematch negotiation, for instance)
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

## Rematch negotiation

When the opponent proposes or declines a rematch,
a `reloadTable` event is sent to the client.
You should then fetch the game document to learn about
the rematch negotiation state, in `opponent.isOfferingRematch`.

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
