const coll = db.game_search;

const players = [
  'penguingim1',
  'aladdin65',
  'aleksey472',
  'azuaga',
  'benpig',
  'blackboarder',
  'bockosrb555',
  'bogdan_low_player',
  'charlytb',
  'chchbbuur',
  'chessexplained',
  'cmcookiemonster',
  'crptone',
  'cselhu3',
  'darkzam',
  'dmitri31',
  'dorado99',
  'ericrosen',
  'fast-tsunami',
  'flaneur',
];
const sources = [1, 2, 3, 4, 5, 6, 7, 8];
const variants = [1, 2, 3, 4, 5, 6, 7, 8];
const results = [1, 2, 3, 4, 5, 6, 7, 8];
const winColor = [true, false, null];

const nbGames = 1000 * 1000;

function intRandom(max) {
  return Math.floor(Math.random() * max);
}
function anyOf(as) {
  return as[intRandom(as.length)];
}

coll.drop();

print(`Adding ${nbGames} games`);
for (let i = 0; i < nbGames; i++) {
  const users = [anyOf(players), anyOf(players)];
  const winnerIndex = intRandom(2);
  coll.insert({
    users: users,
    winner: users[winnerIndex],
    loser: users[1 - winnerIndex],
    winColor: anyOf(winColor),
    avgRating: NumberInt(600 + intRandom(2400)),
    source: NumberInt(anyOf(sources)),
    variants: NumberInt(anyOf(variants)),
    mode: !!intRandom(2),
    turns: NumberInt(1 + intRandom(300)),
    minutes: NumberInt(30 + intRandom(3600 * 3)),
    clock: {
      init: NumberInt(0 + intRandom(10800)),
      inc: NumberInt(0 + intRandom(180)),
    },
    result: anyOf(results),
    date: new Date(Date.now() - intRandom(118719488)),
    analysed: !!intRandom(2),
  });
  if (i % 1000 == 0) print(`${i} / ${nbGames}`);
}

const indexes = [
  { users: 1 },
  { winner: 1 },
  { loser: 1 },
  { winColor: 1 },
  { avgRating: 1 },
  { source: 1 },
  { variants: 1 },
  { mode: 1 },
  { turns: 1 },
  { minutes: 1 },
  { 'clock.init': 1 },
  { 'clock.inc': 1 },
  { result: 1 },
  { date: 1 },
  { analysed: 1 },
];

print('Adding indexes');
indexes.forEach(index => {
  printjson(index);
  db.game_search.createIndex(index);
});

print('Searching');
db.game_search
  .find({
    avgRating: { $gt: 1000 },
    turns: { $lt: 250 },
    'clock.init': { $gt: 1 },
    minutes: { $gt: 2, $lt: 150 },
  })
  .sort({
    date: -1,
  })
  .limit(20)
  .explain('executionStats');

/*
   "executionStats": {
    "executionSuccess": true,
    "nReturned": 7613,
    "executionTimeMillis": 2279,
    "totalKeysExamined": 1000000,
    "totalDocsExamined": 1000000,
    "executionStages": {
      "stage": "FETCH",
      "filter": {
        "$and": [
          {
            "minutes": {
              "$lt": 150
            }
          },
          {
            "turns": {
              "$lt": 250
            }
          },
          {
            "avgRating": {
              "$gt": 1000
            }
          },
          {
            "clock.init": {
              "$gt": 1
            }
          },
          {
            "minutes": {
              "$gt": 2
            }
          }
        ]
      },
      "nReturned": 7613,
      "executionTimeMillisEstimate": 160,
      "works": 1000001,
      "advanced": 7613,
      "needTime": 992387,
      "needYield": 0,
      "saveState": 8151,
      "restoreState": 8151,
      "isEOF": 1,
      "invalidates": 0,
      "docsExamined": 1000000,
      "alreadyHasObj": 0,
      "inputStage": {
        "stage": "IXSCAN",
        "nReturned": 1000000,
        "executionTimeMillisEstimate": 20,
        "works": 1000001,
        "advanced": 1000000,
        "needTime": 0,
        "needYield": 0,
        "saveState": 8151,
        "restoreState": 8151,
        "isEOF": 1,
        "invalidates": 0,
        "keyPattern": {
          "date": 1
        },
        "indexName": "date_1",
        "isMultiKey": false,
        "multiKeyPaths": {
          "date": [ ]
        },
        "isUnique": false,
        "isSparse": false,
        "isPartial": false,
        "indexVersion": 2,
        "direction": "backward",
        "indexBounds": {
          "date": [
            "[MaxKey, MinKey]"
          ]
        },
        "keysExamined": 1000000,
        "seeks": 1,
        "dupsTested": 0,
        "dupsDropped": 0,
        "seenInvalidated": 0
      }
    }
*/
