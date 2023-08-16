var puzzles = db.puzzle;

modified = 0;

puzzles
  .find({
    'vote.ratio': { $exists: false },
  })
  .forEach(function (p) {
    puzzles.update(
      {
        _id: p._id,
      },
      {
        $set: {
          'vote.ratio': NumberInt((100 * (p.vote.up - p.vote.down)) / (p.vote.up + p.vote.down)),
          'vote.nb': NumberInt(p.vote.up + p.vote.down),
        },
        $unset: {
          'vote.sum': true,
        },
      },
    );
    modified += 1;
  });

print(modified);
