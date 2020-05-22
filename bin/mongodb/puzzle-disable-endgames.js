var puzzles = db.puzzle;
var count = 0;
puzzles.find({
  $and: [{
    _id: {
      $gt: 60121
    },
    $or: [{
      'vote.nb': {
        $lt: 30
      },
      'vote.ratio': {
        $lt: 50
      }
    }]
  }]
}).forEach(function(p) {
  var parts = p.fen.split(/\s/);
  var pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;
  if (pieceCount < 9) {
    count++;
    puzzles.update({
      _id: p._id
    }, {
      $set: {
        vote: {
          up: NumberInt(0),
          down: NumberInt(9000),
          nb: NumberInt(9000),
          ratio: NumberInt(-100)
        }
      }
    });
  }
});
print("Disabled " + count + " puzzles");
