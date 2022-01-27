// for puzzle v2

db.puzzle2_path.createIndex({ min: 1, max: -1 });

db.puzzle2_round.createIndex({ p: 1 }, { partialFilterExpression: { t: { $exists: true } } });
db.puzzle2_round.createIndex({ u: 1, d: -1 }, { partialFilterExpression: { u: { $exists: 1 } } });

db.puzzle2_puzzle.createIndex({ day: 1 }, { partialFilterExpression: { day: { $exists: true } } });
