[lichess.org](https://lichess.org) [![Build Status](https://travis-ci.org/ornicar/lila.svg?branch=master)](https://travis-ci.org/ornicar/lila)
---------------------------------

<img src="https://raw.githubusercontent.com/ornicar/lila/master/public/images/homepage_light.1200.png" alt="lichess.org" />

Lila is a free online chess game server focused on [realtime](https://lichess.org/games) gameplay and ease of use.

It features a [search engine](https://lichess.org/games/search),
[computer analysis](https://lichess.org/ief49lif) distributed with [fishnet](https://github.com/niklasf/fishnet),
[tournaments](https://lichess.org/tournament),
[simuls](https://lichess.org/simul),
[forums](https://lichess.org/forum),
[teams](https://lichess.org/team),
[tactic trainer](https://lichess.org/training),
[opening trainer](https://lichess.org/training/opening),
a [mobile app](https://lichess.org/mobile),
and a [network world map](https://lichess.org/network).
The UI is available in more than [80 languages](https://lichess.org/translation/contribute) thanks to the community.

Lichess is written in [Scala 2.11](https://www.scala-lang.org/),
and relies on [Play 2.3](https://www.playframework.com/) for the routing, templating, and JSON.
Pure chess logic is contained in [scalachess](https://github.com/ornicar/scalachess) submodule.
The codebase is fully asynchronous, making heavy use of Scala Futures and [Akka 2 actors](http://akka.io).
Lichess talks to [Stockfish](http://stockfishchess.org/) deployed in an AI cluster of donated servers.
It uses [MongoDB 2.6](https://mongodb.org) to store more than 68 million games, which are indexed by [elasticsearch](http://elasticsearch.org).
HTTP requests and websocket connections are proxied by [nginx 1.6](http://nginx.org).
Client-side is written in [mithril.js](http://mithril.js.org/).
The [blog](https://lichess.org/blog) uses a free open content plan from [prismic.io](https://prismic.io).

Join us on #lichess IRC channel on freenode for more info.
Use [github issues](https://github.com/ornicar/lila/issues) for bug reports and feature requests.

Installation
------------

> If you want to add a live chess section to your website, you are welcome to [embed lichess](https://lichess.org/developers) into your website. It's very easy to do.

> This project source code is open for other developers to have an example of non-trivial scala/play2/mongodb application. You're welcome to reuse as much code as you want for your projects and to get inspired by the solutions I propose to many common web development problems. But please don't just create a public lichess clone. Instead, [embed lichess using an &lt;iframe&gt;](https://lichess.org/developers) into your website.

> Also note that if I provide the source code, I do **not** offer support for your lichess instance. I will probably ignore any question about lichess installation and runtime issues.

## HTTP API

Feel free to use lichess API in your applications and websites.

If the resource you need is not available yet,
drop us an email at contact@lichess.org and we'll discuss it.

### API Limits

To respect the API servers and avoid an IP ban, please wait 1 second between requests. If you receive an HTTP response with a [429 status](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes#429), please wait a full minute before resuming API usage.

### `GET /api/user/<username>` fetch one user

```
> curl https://en.lichess.org/api/user/thibault
```

```javascript
{
  "username": "thibault",
  "title": null,                             // chess title like FM or LM (lichess master)
  "url": "https://lichess.org/@/thibault",   // profile url
  "online": true,                            // is the player currently using lichess?
  "playing": "https://lichess.org/abcdefgh", // game being played, if any
  "engine": false,                           // true if the user is known to use a chess engine
  "language": "en",                          // prefered language
  "profile": {
    "bio": "Writes bugs for free",
    "country": "FR",
    "firstName": "Thibault",
    "lastName": "Duplessis",
    "location": "Paris"
  },
  "perfs": {                                 // user performances in different games
    "bullet": {
      "games": 35,                           // number of rated games played
      "rating": 1624,                        // Glicko2 rating
      "rd": 80,                              // Glicko2 rating deviation
      "prog": -13                            // progress over the last twelve games
    },
    "chess960": {
      "games": 1,
      "rating": 1739,
      "rd": 277,
      "prog": 33
    },
    "classical": {
      "games": 331,
      "rating": 1603,
      "rd": 65,
      "prog": 9
    },
    "kingOfTheHill": {
      "games": 3,
      "rating": 1622,
      "rd": 223,
      "prog": -62
    },
    "puzzle": {
      "games": 9,
      "rating": 902,
      "rd": 117,
      "prog": -328
    },
    "threeCheck": {
      "games": 1,
      "rating": 1662,
      "rd": 290
    }
  }
}
```

Example usage with JSONP:

```javascript
$.ajax({
  url:'https://en.lichess.org/api/user/thibault',
  dataType:'jsonp',
  jsonp:'callback',
  success: function(data) {
    // data is a javascript object, do something with it!
    console.debug(JSON.stringify(data));
  }
});
```

### `GET /api/user` fetch many users

```
> curl https://en.lichess.org/api/user?team=coders&nb=10&page=1
```

The team parameter is mandatory.

name | type | default | description
--- | --- | --- | ---
**team** | string | - | filter users by team
**nb** | int | 10 | maximum number of users to return per page
**page** | int | 1 | for pagination

```javascript
{
  "currentPage": 3,
  "previousPage": 2,
  "nextPage": 4,
  "maxPerPage": 100,
  "nbPages": 43,
  "nbResults": 4348,
  "currentPageResults": [
    {
      ... // see user document above
    },
    ... // other users
  ]
}
```

Example usage with JSONP:

```javascript
$.ajax({
  url:'https://en.lichess.org/api/user',
  data: {
    team: 'coders',
    nb: 100
  },
  dataType:'jsonp',
  jsonp:'callback',
  success: function(data) {
    // data is a javascript object, do something with it!
    console.debug(JSON.stringify(data.list));
  }
});
```

### `GET /api/user/<username>/games` fetch user games

```
> curl https://en.lichess.org/api/user/thibault/games?nb=10&page=2
```

Games are returned by descendant chronological order.
All parameters are optional.

name | type | default | description
--- | --- | --- | ---
**nb** | int | 10 | maximum number of games to return per page
**page** | int | 1 | for pagination
**with_analysis** | 1 or 0 | 0 | include deep analysis data in the result
**with_moves** | 1 or 0 | 0 | include a list of PGN moves
**with_opening** | 1 or 0 | 0 | include opening informations
**with_movetimes** | 1 or 0 | 0 | include move time informations
**rated** | 1 or 0 | - | rated games only
**playing** | 1 or 0 | - | games in progress only

```javascript
{
  "currentPage": 3,
  "previousPage": 2,
  "nextPage": 4,
  "maxPerPage": 100,
  "nbPages": 43,
  "nbResults": 4348,
  "currentPageResults": [
    {
      "id": "39b12Ikl",
      "variant": "chess960", // standard/chess960/fromPosition/kingOfTheHill/threeCheck
      "speed": "blitz", // bullet|blitz|classical|unlimited
      "perf": "chess960", // bullet|blitz|classical|chess960|kingOfTheHill|threeCheck
      "rated": true,
      "status": "mate", // (1)
      "clock":{          // all clock values are expressed in seconds
        "initial": 300,
        "increment": 8,
        "totalTime": 540  // evaluation of the game duration = initial + 30 * increment
      },
      "createdAt": 1389100907239,
      "lastMoveAt": 1389100907239,
      "turns": 44,
      "color": "white", // color who plays next
      "url": "https://lichess.org/x2kpaixn",
      "winner": "black",
      "players": {
        "white": {
          "userId": "thibault",
          "name": "Thibault D", // only in the case of imported game
          "rating": 1642,
          "analysis": {
            "blunder": 1,
            "inaccuracy": 0,
            "mistake": 2
          },
        // rounded move times in tenths of seconds
        "moveTimes":[30,40,10,40,40,100,50,200,400,150,150,40,50,200,80]
        },
        "black": ... // other player
      }
      "analysis": [ // only if the with_analysis flag is set
        {
          "eval": -26, // board evaluation in centipawns
          "move": "e4"
        },
        {
          "eval": -8,
          "move": "b5"
        },
        {
          "eval": -66,
          "move": "Nfe3",
          "variation": "c4 bxc4 Nfe3 c5 Qf1 f6 Rxc4 Bb7 b4 Ba6"
        },
        // ... more moves
      ],
      "moves": "Nf3 d5 g3 e6 Bg2 Be7 d3 Nf6 Nbd2 O-O O-O c6 Rfe1 b6 e4 Bb7", // with_moves flag
      "opening": { // with_opening flag
        "code": "A07",
        "name": "King's Indian Attack, General"
      }
    },
    {
      ... // other game
    }
  ]
}
```

(1) All game statuses: https://github.com/ornicar/scalachess/blob/master/src/main/scala/Status.scala#L16-L25

### `GET /api/game/{id}` fetch one game by ID

```
> curl https://en.lichess.org/api/game/x2kpaixn
```

A single game is returned.
All parameters are optional.

name | type | default | description
--- | --- | --- | ---
**with_analysis** | 1 or 0 | 0 | include deep analysis data in the result
**with_moves** | 1 or 0 | 0 | include a list of PGN moves
**with_movetimes** | 1 or 0 | 0 | include move time informations
**with_opening** | 1 or 0 | 0 | include opening informations
**with_fens** | 1 or 0 | 0 | include a list of FEN states

```javascript
{
  "id": "39b12Ikl",
  "initialFen": "rkrqnnbb/pppppppp/8/8/8/8/PPPPPPPP/RKRQNNBB w KQkq - 0 1" // omitted is standard
  "variant": "chess960", // standard/chess960/fromPosition/kingofthehill/threeCheck
  "speed": "blitz", // bullet|blitz|classical|unlimited
  "perf": "chess960", // bullet|blitz|classical|chess960|kingOfTheHill|threeCheck
  "rated": true,
  "status": "mate", // (1)
  "clock":{          // all clock values are expressed in seconds
    "initial": 300,
    "increment": 8,
    "totalTime": 540  // evaluation of the game duration = initial + 30 * increment
  },
  "createdAt": 1389100907239,
  "lastMoveAt": 1389100907239,
  "turns": 44,
  "color": "white", // color who plays next
  "url": "https://lichess.org/x2kpaixn",
  "winner": "black",
  "players": {
    "white": {
      "userId": "thibault",
      "name": "Thibault D", // only in the case of imported game
      "rating": 1642,
      "analysis": {
        "blunder": 1,
        "inaccuracy": 0,
        "mistake": 2
      },
      // rounded move times in tenths of seconds
      "moveTimes":[30,40,10,40,40,100,50,200,400,150,150,40,50,200,80]
    },
    "black": ... // other player
  },
  "analysis": [ // only if the with_analysis flag is set
    {
      "eval": -26, // board evaluation in centipawns
      "move": "e4"
    },
    {
      "eval": -8,
      "move": "b5"
    },
    {
      "eval": -66,
      "move": "Nfe3",
      "variation": "c4 bxc4 Nfe3 c5 Qf1 f6 Rxc4 Bb7 b4 Ba6"
    },
    // ... more moves
  ],
  "moves": "Nf3 d5 g3 e6 Bg2 Be7 d3 Nf6 Nbd2 O-O O-O c6 Rfe1 b6 e4 Bb7", // with_moves flag
  "opening": { // with_opening flag
    "code": "A07",
    "name": "King's Indian Attack, General"
  }
  "fens": [ // only if with_fens flag is set
      "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R",
      "rnbqkbnr/ppp1pppp/8/3p4/8/5N2/PPPPPPPP/RNBQKB1R",
      "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R",
      "rnbqkbnr/ppp2ppp/4p3/3p4/8/5NP1/PPPPPP1P/RNBQKB1R",
      "rnbqkbnr/ppp2ppp/4p3/3p4/8/5NP1/PPPPPPBP/RNBQK2R",
      "rnbqk1nr/ppp1bppp/4p3/3p4/8/5NP1/PPPPPPBP/RNBQK2R",
      // ... more fens
  ]
}
```

(1) All game statuses: https://github.com/ornicar/scalachess/blob/master/src/main/scala/Status.scala#L16-L25

### `GET /game/export/{id}.pgn` fetch one game PGN by ID

https://en.lichess.org/game/export/Qa7FJNk2.pgn

This returns the raw PGN for a game.

```
[Event "Rated game"]
[Site "https://lichess.org/Qa7FJNk2"]
[Date "2014.06.12"]
[White "onpurplesz"]
[Black "LauraSchmidt"]
[Result "1-0"]
[WhiteElo "1516"]
[BlackElo "1698"]
[PlyCount "37"]
[Variant "Standard"]
[TimeControl "1020+0"]
[ECO "C50"]
[Opening "Italian Game, General"]
[WhiteClock ":11:9"]
[BlackClock ":14:6"]
[Annotator "lichess.org"]

1. e4 e5 2. Nf3 Nc6 3. Bc4 { Italian Game, General } Qf6?! { (0.13 → 0.98) Inaccuracy. Best move was Nf6. } (3... Nf6 4. d3 Bc5 5. O-O O-O 6. Bg5 Be7 7. a3 d6 8. h3 a6) 4. d3 h6 5. Be3 d6 6. h3?! { (0.84 → 0.31) Inaccuracy. The best move was Nc3. } (6. Nc3 Be6 7. Nd5 Qd8 8. d4 exd4 9. Nxd4 Nxd4 10. Qxd4 c6 11. Nc3 Bxc4 12. Qxc4 Nf6 13. O-O Be7) 6... a6 7. Nbd2 Be6 8. Qe2 Bxc4 9. Nxc4 Nge7 10. a3 Nd4?! { (0.29 → 0.79) Inaccuracy. The best move was O-O-O. } (10... O-O-O 11. O-O g5 12. a4 Bg7 13. Bd2 Kb8 14. Rae1 Qe6 15. b4 f5 16. b5 fxe4) 11. Bxd4 exd4 12. O-O-O Nc6 13. Rhe1 O-O-O 14. e5 dxe5 15. Nfxe5 Nxe5 16. Qxe5 Qxe5? { (0.35 → 1.78) Mistake. The best move was Qxf2. } (16... Qxf2 17. Re2 Qf6 18. Qxf6 gxf6 19. Rf1 Bg7 20. Nd2 h5 21. Ne4 Rhe8 22. Ref2 Re5 23. b3 Rdd5 24. Kb2) 17. Nxe5 Rg8?! { (1.76 → 2.32) Inaccuracy. The best move was Rd7. } (17... Rd7 18. Nxd7 Kxd7 19. Re4 c5 20. c3 dxc3 21. bxc3 Bd6 22. Kc2 b5 23. a4 Ra8 24. d4 Kc6 25. dxc5) 18. Nxf7 Rd7? { (2.35 → Mate in 2) Checkmate is now unavoidable. The best move was Rd5. } (18... Rd5 19. Re8+ Kd7 20. Rde1 Bb4 21. Rxg8 Bxe1 22. Rxg7 Bxf2 23. Nxh6+ Kc6 24. Kd1 Rb5 25. b3 Rh5 26. Nf7) 19. Re8+ { Black resigns } 1-0
```

### `GET /api/tournament` fetch current tournaments

Returns tournaments displayed on the schedule: https://lichess.org/tournament

```
> curl https://en.lichess.org/api/tournament
```

```javascript
{
  "created": [
    {
      "id": "f4wnl48m",
      "createdBy": "thedave13213",
      "system": "arena",
      "minutes": 45,
      "clock": {
        "limit": 120,
        "increment": 0
      },
      "position": null,
      "rated": true,
      "fullName": "Ulrich Arena",
      "nbPlayers": 1,
      "private": false,
      "variant": {
        "key": "standard",
        "short": "Std",
        "name": "Standard"
      },
      "secondsToStart": 38,
      "startsAt": 1471257146633,
      "finishesAt": 1471259846633,
      "status": 10,
      "schedule": null,
      "winner": null,
      "conditions": null,
      "perf": {
        "icon": "T",
        "name": "Bullet",
        "position": 0
      }
    }
  ],
  "started": [
    ...
  ],
  "finished": [
    ...
  ]
}
```

JSONP is available.

### `GET /api/tournament/<tournamentId>` fetch one tournament

Returns tournament info, and a page of the tournament standing

name | type | default | description
--- | --- | --- | ---
**page** | int | 1 | for standing pagination

```
curl 'https://en.lichess.org/api/tournament/x5WNIngd?page=1'
```

```javascript
{
  "clock": {
    "increment": 0,
    "limit": 300
  },
  "createdBy": "lichess",
  "fullName": "Hourly Blitz Arena",
  "id": "x5WNIngd",
  "isFinished": true,
  "isStarted": false,
  "minutes": 56,
  "nbPlayers": 128,
  "next": {
    "finishesAt": null,
    "id": "dNivtcYG",
    "name": "Hourly SuperBlitz Arena",
    "nbPlayers": 0,
    "perf": {
      "icon": ")",
      "name": "Blitz"
    },
    "startsAt": "2016-08-15T11:00:00.000Z"
  },
  "startsAt": "2016-05-25T22:00:00.000Z",
  "system": "arena",
  "variant": "standard",
  "verdicts": {
    "accepted": true,
    "list": []
  }
  "pairings": [
    {
      "id": "gtFxbHU8",
      "s": 3,
      "u": ["nimsraw", "Ernom"]
    },
    {
      "id": "oyl3n1bB",
      "s": 3,
      "u": ["esparzaesc72", "athletics_champ"]
    },
    ...
  ],
  "perf": {
    "icon": ")",
    "name": "Blitz"
  },
  "podium": [
    {
      "name": "athletics_champ",
      "nb": {
        "berserk": 0,
        "game": 7,
        "win": 7
      },
      "performance": 2297,
      "rank": 1,
      "rating": 2100,
      "ratingDiff": 37,
      "score": 24,
      "sheet": {
      "fire": true,
      "scores": [
        [2, 2],
        ...
      ]
    },
    ...
  ],
  "schedule": {
    "freq": "hourly",
    "speed": "blitz"
  },
  "standing": {
    "page": 1,
    "players": [
      {
        "name": "athletics_champ",
        "rank": 1,
        "rating": 2100,
        "ratingDiff": 37,
        "score": 24,
        "sheet": {
          "fire": true,
          "scores": [
            [2, 2 ],
            ...
          ]
        }
      },
      ...
    ]
  }
}
```

JSONP is available.

Credits
-------

See the [lichess Thanks page](https://lichess.org/thanks)

Supported browsers
------------------

- Firefox, 6 months old or newer
- Chrome, 6 months old or newer

These two are the best, hands down. Use them if you can.

We also do our best to support:

- Internet Explorer 11
- Microsoft Edge
- Opera 34 and newer
- Safari 9 and newer

Others and older browsers are guaranteed not to work with lichess and we don't care.

Licence
-------

Lila is realeased under the MIT license.
