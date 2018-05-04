# TODO migrate me to /api

### `GET /api/games/vs/<username>/<username>` fetch games between 2 users

```
> curl https://lichess.org/api/games/vs/thibault/legend?nb=10&page=2
```

Parameters and result are similar to the users games API.

### `GET /api/games/team/<teamId>` fetch games between players of a team

```
> curl https://lichess.org/api/games/team/freenode?nb=10&page=2
```

Parameters and result are similar to the users games API.

### `GET /api/game/{id}` fetch one game by ID

```
> curl https://lichess.org/api/game/x2kpaixn
```

A single game is returned.
All parameters are optional.

name | type | default | description
--- | --- | --- | ---
**with_analysis** | 1 or 0 | 0 | include deep analysis data in the result
**with_moves** | 1 or 0 | 0 | include a list of PGN moves
**with_movetimes** | 1 or 0 | 0 | include move time information
**with_opening** | 1 or 0 | 0 | include opening information
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
    "totalTime": 620  // evaluation of the game duration = initial + 40 * increment
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
      // time taken for each move in hundreths of seconds
      "moveCentis": [0, 812, 2516, 7644, 12660, 15740, 4044, ...]
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

(1) All game statuses: https://github.com/ornicar/scalachess/blob/master/src/main/scala/Status.scala#L16-L28

### `POST /api/games` fetch many games by ID

```
> curl --data "x2kpaixn,gtSLJGOK" 'https://lichess.org/api/games'
```

Games are returned in the order same order as the ids.
All parameters are optional.

name | type | default | description
--- | --- | --- | ---
**with_moves** | 1 or 0 | 0 | include a list of PGN moves

### `GET /api/tournament/<tournamentId>` fetch one tournament

Returns tournament info, and a page of the tournament standing

name | type | default | description
--- | --- | --- | ---
**page** | int | 1 | for standing pagination

```
curl 'https://lichess.org/api/tournament/x5WNIngd?page=1'
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
