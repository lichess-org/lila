// for puzzle v2

db.puzzle2_path.createIndex({min: 1, max: -1});
db.puzzle2_round.createIndex({p: 1}, {partialFilterExpression:{t:{$exists:true}}});
