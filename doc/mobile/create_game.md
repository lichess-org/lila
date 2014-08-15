# Create a game

## With A.I.

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
    "user_id": "ozzie"        // request more info at /api/user/ozzie
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

## Seek for human opponent

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
