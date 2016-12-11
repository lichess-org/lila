var puzzles = db.puzzle;

modified = 0;

puzzles.find().forEach(function(p) {
  puzzles.update({
    _id: p._id
  }, {
    $set: {
      "vote.ratio": Math.floor(100*(p.vote.up - p.vote.down)/(p.vote.up + p.vote.down)),
      "vote.nb": (p.vote.up + p.vote.down)
    }
  });
  modified += 1;
});

print(modified);