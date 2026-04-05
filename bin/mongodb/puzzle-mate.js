db.puzzle.find({ tags: { $exists: true } }).forEach(function (o) {
  db.puzzle.update(
    {
      _id: o._id,
    },
    {
      $set: {
        mate: o.tags.includes('forced mate'),
      },
      $unset: {
        tags: true,
      },
    },
  );
});
