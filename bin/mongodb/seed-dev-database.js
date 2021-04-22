let users = db.user4;

// Add some users (their passwords are "password")
users.updateOne({ "_id" : "limezebra"},
{
  "$set":{
     "username":"LimeZebra",
     "email":"limezebra@gmail.com",
     "bpass":BinData(0,"n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ"),
     "perfs":{

     },
     "count":{
        "ai":0,
        "draw":0,
        "drawH":0,
        "game":0,
        "loss":0,
        "lossH":0,
        "rated":0,
        "win":0,
        "winH":0
     },
     "enabled":true,
     "createdAt":ISODate("2021-04-22T19:48:34.144Z"),
     "seenAt":ISODate("2021-04-22T19:48:34.858Z"),
     "time":{
        "total":0,
        "tv":0
     },
     "verbatimEmail":"limeZebra@gmail.com",
     "len":9,
     "lang":"en-US"
  }
}
, {upsert: true});

users.updateOne({ "_id" : "humorousantelope"},
{
  "$set":{
     "username":"HumorousAntelope",
     "email":"humorousantelope@gmail.com",
     "bpass":BinData(0,"n1G7smu1Z6DC1GszOpLba+POwqXfToPW8KMVJWxzECou5EnPYotZ"),
     "perfs":{

     },
     "count":{
        "ai":0,
        "draw":0,
        "drawH":0,
        "game":0,
        "loss":0,
        "lossH":0,
        "rated":0,
        "win":0,
        "winH":0
     },
     "enabled":true,
     "createdAt":ISODate("2021-04-22T19:48:34.144Z"),
     "seenAt":ISODate("2021-04-22T19:48:34.858Z"),
     "time":{
        "total":0,
        "tv":0
     },
     "verbatimEmail":"HumorousAntelope@gmail.com",
     "len":9,
     "lang":"en-US"
  }
}
, {upsert: true});

// add some games
let games = db.game5;

games.updateOne({ "_id" : "D9f2fvLt"},
{
  "$set":{
   "is" : "aaaaaa",
   "us" : [
           "LimeZebra",
           "HumorousAntelope"
   ],
   "p0" : {
           "e" : 1760,
           "p" : true,
           "d" : 119
   },
   "p1" : {
           "e" : 1728,
           "d" : -5
   },
   "s" : 31,
   "t" : 43,
   "c" : BinData(0,"CgAANI8AKqw="),
   "cw" : BinData(0,"ViGoXhaqKJqy6K9rRsLIsJG9dB7Whfk5OIpRuqTI"),
   "cb" : BinData(0,"VSyJozR7hL6SNDZNEKQgi/PHAS5pL6SnwZus4A=="),
   "ra" : true,
   "ca" : ISODate("2020-07-01T02:25:33.258Z"),
   "ua" : ISODate("2020-07-01T02:29:47.068Z"),
   "so" : 12,
   "hp" : BinData(0,"PDdrGSJaqs+KmUAq85NNMThkXKNHIUA="),
   "w" : true,
   "wid" : "LimeZebra",
   "an" : true
   }
}
, {upsert: true});

games.updateOne({"_id" : "TaHSAsYD"},
{
   "$set":{
   "is" : "O8dEwDod",
   "us" : [
           "LimeZebra",
           "HumorousAntelope"
   ],
   "p0" : {
           "e" : 2264,
           "d" : 3
   },
   "p1" : {
           "e" : 2069,
           "d" : -3
   },
   "s" : 35,
   "t" : 97,
   "c" : BinData(0,"CgAA38IA6mQ="),
   "cw" : BinData(0,"wGAhen8MW6C28eG7gbi0S7rEzAvMPLTu1KHlhKUh18BT3zxTY9SgQYDTvHlPCf6QIyB8SJ/PQYihGoTkZtLA"),
   "cb" : BinData(0,"vHARnEBZLL6hkblax4GSWiwXenwwssK8dk8LyS4sfqG7D1eo43kerh70+MHF8Bm0ah3leztkZ5KI4yrsrCrO8piEWFA="),
   "ra" : true,
   "ca" : ISODate("2020-07-01T02:23:07.519Z"),
   "ua" : ISODate("2020-07-01T02:42:59.641Z"),
   "so" : 5,
   "tid" : "f5WSE8Yc",
   "hp" : BinData(0,"PDVvHX7zZXUV51aObVt//n+PYCv+TKzL6zXvcTbiqlrv551I5GeN/S1rL74i/K9a68ahbTaRzHvo9uTN2+dc"),
   "w" : true,
   "wid" : "LimeZebra",
   "an" : true
   }
}
, {upsert: true});

games.updateOne({"_id" : "rVK1n3ZW"},
{
   "$set":{
   "is" : "YQjJlsJm",
   "us" : [
           "HumorousAntelope",
           "LimeZebra"
   ],
   "p0" : {
           "e" : 1728,
           "d" : 19
   },
   "p1" : {
           "e" : 1634,
           "d" : -14
   },
   "s" : 31,
   "t" : 75,
   "c" : BinData(0,"CgAAaY4AcFY="),
   "cw" : BinData(0,"lEpQfow+jiH0ZqKUtise1vRboSseBmxcOHuCdrbcfoG54kZkOWRyS4Y4GZKj8bBNJhEA"),
   "cb" : BinData(0,"lFBoqBHuiqOYaqOE5i4e5PibwfhOF80wIGgPgfHuHw/D5HdI1yOMdEXIqOBIs40A"),
   "ra" : true,
   "ca" : ISODate("2020-05-01T08:53:16.559Z"),
   "ua" : ISODate("2020-05-01T09:02:37.125Z"),
   "so" : 5,
   "tid" : "7VGVwRoa",
   "hp" : BinData(0,"AlG1u0hHOm25JpJokI+8K3RkyR0MmzAXP2rScXHxpee2Ad7au+S6MxnE"),
   "w" : true,
   "wid" : "HumorousAntelope",
   "an" : true
   }
}
, {upsert: true});

// add some puzzles
let puzzles = db.puzzle2_puzzle;

puzzles.updateOne({ "_id" : "02VMp"},
{
  "$set":{
      "gameId" : "D9f2fvLt",
      "fen" : "rk1q3r/4nQpp/p2pB3/1p6/4P3/5P1P/PPP2P2/2KR3R w - - 0 20",
      "themes" : [
              "middlegame",
              "defensiveMove",
              "hangingPiece",
              "long",
              "advantage"
      ],
      "glicko" : {
              "r" : 1532.7572615553727,
              "d" : 129.23809133936328,
              "v" : 0.08996277354273302
      },
      "plays" : 17,
      "vote" : 0.7272727272727273,
      "line" : "d1d6 d8d6 h1d1 d6c5 f7g7 e7g6",
      "generator" : 14,
      "cp" : 376,
      "vd" : 3,
      "vu" : 19,
      "users" : [
         "LimeZebra",
         "HumorousAntelope"
      ]
   }
}
, {upsert: true});

puzzles.updateOne({"_id" : "01tg7"},
{
   "$set":{
      "gameId" : "TaHSAsYD",
      "fen" : "8/1bnr2pk/4pq1p/p1p1Rp2/P1B2P2/1PP3Q1/3r1BPP/4R1K1 w - - 1 44",
      "themes" : [
              "middlegame",
              "short",
              "fork",
              "advantage"
      ],
      "glicko" : {
              "r" : 1540.7253919742554,
              "d" : 75.23754491869116,
              "v" : 0.09003857959955437
      },
      "plays" : 241,
      "vote" : 0.9266055226325989,
      "line" : "f2c5 d2g2 g3g2 b7g2",
      "generator" : 14,
      "cp" : 468,
      "vd" : 8,
      "vu" : 210,
      "users" : [
         "LimeZebra",
         "HumorousAntelope"
      ]
   }
}
, {upsert: true});

puzzles.updateOne({"_id" : "02yo7"},
{
   "$set":{
         "gameId" : "rVK1n3ZW",
         "fen" : "r1b2rk1/ppp2pp1/2np2qp/1BbNp3/4P3/3P1N1P/PPP2PP1/R2QK2R b KQ - 3 10",
         "themes" : [
                 "opening",
                 "short",
                 "fork",
                 "crushing"
         ],
         "glicko" : {
                 "r" : 1116.0797837293023,
                 "d" : 104.13245164404374,
                 "v" : 0.08999051112782643
         },
         "plays" : 32,
         "vote" : 0.6666666865348816,
         "line" : "c6b4 d5e7 g8h7 e7g6",
         "generator" : 14,
         "cp" : 717,
         "vd" : 1,
         "vu" : 5,
         "users" : [
            "HumorousAntelope",
            "LimeZebra"
         ]
   }
}
, {upsert: true});


print('Done!');
