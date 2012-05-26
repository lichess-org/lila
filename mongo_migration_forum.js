// categories
var categSlugs = {};
var topicIds = {};

(function(oldColl, coll) {
  print("Categs");
  var cursor = oldColl.find(), nb = 0;
  coll.drop();
  while(cursor.hasNext()) {
    var obj = cursor.next();
    categSlugs[obj._id] = obj.slug;
    coll.insert({
      _id: obj.slug,
      pos: obj.position,
      name: obj.name,
      desc: obj.description
    });
    nb ++;
  }
  coll.ensureIndex({pos: 1}, {unique: true});
  print("Done categs: " + nb);
})(db.forum_category, db.f_categ);

(function(oldColl, coll) {
  print("Topics");
  var cursor = oldColl.find(), nb = 0;
  coll.drop();
  while(cursor.hasNext()) {
    var obj = cursor.next();
    var id = makeId(8);
    topicIds[obj._id] = id;
    coll.insert({
      _id: id,
      slug: obj.slug,
      categ: categSlugs[obj.category["$id"]],
      createdAt: obj.createdAt,
      updatedAt: obj.pulledAt,
      views: obj.numViews,
      name: obj.subject
    });
    nb ++;
  }
  coll.ensureIndex({categ: 1, slug: 1}, {unique: true});
  coll.ensureIndex({categ: 1});
  coll.ensureIndex({categ: 1, updatedAt: -1});
  print("Done topics: " + nb);
})(db.forum_topic, db.f_topic);

(function(oldColl, coll) {
  print("Posts");
  var cursor = oldColl.find(), nb = 0;
  coll.drop();
  while(cursor.hasNext()) {
    var obj = cursor.next();
    var post = {
      _id: makeId(8),
      topic: topicIds[obj.topic["$id"]],
      createdAt: obj.createdAt,
      author: obj.authorName,
      text: obj.message
    };
    if (obj.author) {
      post.user = obj.author;
    }
    coll.insert(post);
    nb ++;
  }
  coll.ensureIndex({topic: 1});
  coll.ensureIndex({topic: 1, createdAt: -1});
  print("Done posts: " + nb);
})(db.forum_post, db.f_post);

function makeId(size) {
  var text = "";
  var possible = "abcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < size; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}
