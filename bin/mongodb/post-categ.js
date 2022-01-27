print('Migrating forum posts');

db.f_post.dropIndex({ categId: 1 });

db.f_categ.find().forEach(function (categ) {
  db.f_topic.find({ categId: categ._id }).forEach(function (topic) {
    db.f_post.find({ topicId: topic._id }).forEach(function (post) {
      db.f_post.update({ _id: post._id }, { $set: { categId: categ._id } }, false, false);
    });
  });
});

print('Building indexes');
db.f_post.ensureIndex({ categId: 1 });

print('Done!');
