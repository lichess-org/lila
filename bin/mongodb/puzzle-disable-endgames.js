var puzzles = db.puzzle;
puzzles.find().forEach(function(p) {
  var parts = p.fen.split(/\s/);
  var pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;
  if (pieceCount < 8) {
    puzzles.update({_id: p._id},{$set:{vote: {sum:-9000}}});
  }
});
