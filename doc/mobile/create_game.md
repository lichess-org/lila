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
  // see document format in the play.md doc file
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

## Challenge someone

```sh
http --form POST en.l.org/setup/friend?user=usernameOrId variant=1 clock=false time=60 increment=60 color=random 'Accept:application/vnd.lichess.v1+json'
```
- color: white | black | random
- variant: 1 (standard) | 2 (chess960) | 3 (from position) | 4 (KotH) | 5 (three-check)
- fen: if variant is 3, any valid FEN string

Response: `201 CREATED`
```javascript
{
  // see document format in the play.md doc file
}
```

Then, connect to the game websocket as a player (see Play documentation).
Now you must send a message over the socket every 1,5 seconds to keep the challenge open:

```javascript
// send
{t: 'challenge', d: 'userIdOfChallengedPlayer'}
```

### Cancel the challenge

A challenge can be aborted before it is accepted, using a HTTP request:

```sh
http --form GET en.l.org/<fullGameId>/cancel 'Accept:application/vnd.lichess.v1+json'
```

### Accepted by the opponent

When the challenge is accepted, you will receive a redirect message through the websocket:

```javascript
// receive
{
  "t": "redirect", // means we should move on to the game
  "id": "abcdefgh1234"
}
```

### Declined by the opponent

When the challenge is decline, you will receive a message through the websocket:

```javascript
// receive
{"t": "declined"}
```

## Receive a challenge

Listen for this message on any websocket:

```javascript
// receive
{
  "t": "challengeReminder",
  "id": "abcdefgh"
}
```

You will receive this every 1,5 seconds aproximatively, until the challenge creator stops sending it.
The challenge ID is also the game public ID.

### Fetch game information

The first time you see a challenge ID, use it to fetch information about the game:

```sh
http GET en.l.org/39b12Ikl 'Accept:application/vnd.lichess.v1+json'
```

Response: `200 OK`
```javascript
{
  // see document format in the Play section
}
```

Use this info to display the challenge.

If you stop receiving the challenge message for some time (lichess web uses 3 seconds),
you should stop displaying it.
