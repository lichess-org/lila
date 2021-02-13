### `GET /api/games/team/<teamId>` fetch games between players of a team

```
> curl https://lichess.org/api/games/team/freenode?nb=10&page=2
```

Parameters and result are similar to the users games API.

### `GET /api/tournament/<tournamentId>` fetch one tournament

Returns tournament info, and a page of the tournament standing

| name     | type | default | description             |
| -------- | ---- | ------- | ----------------------- |
| **page** | int  | 1       | for standing pagination |

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
