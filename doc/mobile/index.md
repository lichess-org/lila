# Mobile API

We use both HTTP and websocket protocols.

## HTTP

All HTTP requests must contain the `Accept: "application/vnd.lichess.v1+json"` header.

Examples use [httpie](https://github.com/jakubroztocil/httpie).

## WEBSOCKET

Websocket connections have the version number in the URL.

### Unique `clientId`

The client is responsible for creating and storing its own unique `clientId`.
It will be sent to the server when connecting to a websocket.

Suggestion of implementation:
```javascript
var clientId = Math.random().toString(36).substring(2);
```

### Message format

All websocket messages, sent or received, are composed of a type `t` and data `d`. Example:

```javascript
{t: 'move', d: {from: 'e2', to: 'e4'}}
```

### Ping

The client should ping the server every second.

```javascript
// send
{t: 'p', v: socketVersion}
```

### Pong

The server answers client pings with a message of type `n`, containing the number of online players.

```javascript
// receive
{t: 'n', d: 1870}
```

The delay between `ping` and `pong` can be used to calculate the client lag.

## API versioning

Current version is v1.

### Changelog

#### v1

work in progress.
