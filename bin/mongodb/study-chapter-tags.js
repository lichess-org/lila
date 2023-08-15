db.study_chapter
  .find({
    // _id: 'MiRw4nlk',
    tags: {
      $exists: false,
    },
  })
  .forEach(function (c) {
    var tags = c.setup.fromPgn ? c.setup.fromPgn.tags : [];
    db.study_chapter.update(
      {
        _id: c._id,
      },
      {
        $set: {
          tags: tags,
        },
        $unset: {
          'setup.fromPgn': !!c.setup.fromPgn,
        },
      },
    );
  });
