var puzzles = db.puzzle;

function fullMoveNumber(p) {
  return Math.floor(1 + p.history.split(' ').length / 2);
}

function changeFenMoveNumber(fen) {
  parts = fen.split(' ');
  if (parts[1] === 'b') return fen;
  parts[5] = parseInt(parts[5]) + 1;
  return parts.join(' ');
}

puzzles.find({
  // _id: 10107
  "_id": {
    "$lt": 60121
  }
}).forEach(function(p) {
  var newFen = changeFenMoveNumber(p.fen);
  puzzles.update({
    _id: p._id
  }, {
    $set: {
      fen: newFen
    }
  });
});
