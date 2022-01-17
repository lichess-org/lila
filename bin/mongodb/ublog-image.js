db.ublog_post
  .find({ image: { $exists: 1 } }, { image: 1 })
  .forEach(p => db.ublog_post.update({ _id: p._id }, { $set: { image: { id: p.image } } }));
