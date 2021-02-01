db.f_categ
  .find({
    pos: {
      $type: 1,
    },
  })
  .forEach(function (o) {
    print(o.name);
    db.f_categ.update(
      {
        _id: o._id,
      },
      {
        $unset: {
          pos: 1,
          nbTopics: 1,
          nbPosts: 1,
          nbTopicsTroll: 1,
          nbPostsTroll: 1,
        },
      },
    );
    db.f_categ.update(
      {
        _id: o._id,
      },
      {
        $set: {
          pos: NumberInt(o.pos),
          nbTopics: NumberInt(o.nbTopics),
          nbPosts: NumberInt(o.nbPosts),
          nbTopicsTroll: NumberInt(o.nbTopicsTroll),
          nbPostsTroll: NumberInt(o.nbPostsTroll),
        },
      },
    );
  });
