var puzzles = db.puzzle;

modified = 0;

puzzles.find().forEach(function(p) {
  puzzles.update({
    _id: p._id
  }, {
    $set: {
      "vote.enabled": (p.vote.up * 3 > p.vote.down) || (p.vote.up + p.vote.down < 50)
    }
  });
  modified += 1;
});

print(modified);