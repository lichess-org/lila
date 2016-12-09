var puzzles = db.puzzle;

puzzles.find({}).forEach(function(p) {
  puzzles.update({
    _id: p._id
  }, {
    $set: {
      voteDisabled: (p.vote.up * 9 < p.vote.down) && (p.vote.up + p.vote.down > 50)
    }
  });
});
