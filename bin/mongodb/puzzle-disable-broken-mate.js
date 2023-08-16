var puzzles = db.puzzle;
var count = 0;

function depthOf(obj) {
  var level = 1;
  var key;

  for (key in obj) {
    if (!obj.hasOwnProperty(key)) continue;

    if (typeof obj[key] === 'object') {
      var depth = depthOf(obj[key]) + 1;
      level = Math.max(depth, level);
    }
  }
  return level;
}

puzzles
  .find({
    mate: true,
    _id: {
      $gt: 60120,
    },
    'vote.sum': {
      $gt: -8000,
    },
  })
  .forEach(function (p) {
    var depth = depthOf(p);
    if (depth % 2 === 1) {
      count++;
      puzzles.update(
        {
          _id: p._id,
        },
        {
          $set: {
            vote: {
              up: NumberInt(0),
              down: NumberInt(9000),
              sum: NumberInt(-9000),
            },
          },
        },
      );
      print(p._id);
    }
  });
print('Disabled ' + count + ' puzzles');
