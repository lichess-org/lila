# Online friends

This section requires authentication.

## Get list of online friends

Request the list of your online friends from any authenticated websocket connection:

```javascript
// send
{t: 'following_onlines'}
```

The server answers with:

```javascript
// receive
{t: "following_onlines", d: ["claymore", "TrialB", "FM amazingoid"]}
```

Note that the full player name is provided, title included.
This allows direct display but requires some gymnastic to infer the user ID:

```javascript
function userFullNameToId(fullName) {
    var split = fullName.split(' ');
    var id = split.length == 1 ? split[0] : split[1];
    return id.toLowerCase();
}
```

## Be notified of friends entering and leaving

The server continually sends these events through any authenticated websocket connection:

```javascript
// receive
{t: "following_enters", d: "Hasimir"}
```

```javascript
// receive
{t: "following_leaves", d: "claymore"}
```

Note that the data is a string, not an array.
