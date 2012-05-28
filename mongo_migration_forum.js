print("Hashing users")
var users = {};
db.user2.find({},{oid:1}).forEach(function(user) {
  users[user.oid.toString()] = user._id;
});

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
    var categId = categSlugs[obj.category["$id"]];
    if (categId == null) {
      print("Skip topic without categ: " + obj.subject);
    } else {
      coll.insert({
        _id: id,
        slug: obj.slug,
        categId: categId,
        createdAt: obj.createdAt,
        updatedAt: obj.pulledAt,
        views: obj.numViews,
        name: obj.subject
      });
    }
    nb ++;
  }
  coll.ensureIndex({categId: 1, slug: 1}, {unique: true});
  coll.ensureIndex({categId: 1});
  coll.ensureIndex({categId: 1, updatedAt: -1});
  print("Done topics: " + nb);
})(db.forum_topic, db.f_topic);

(function(oldColl, coll) {
  print("Posts");
  var cursor = oldColl.find(), nb = 0;
  coll.drop();
  while(cursor.hasNext()) {
    var obj = cursor.next();
    var topicId = topicIds[obj.topic["$id"]];
    if (topicId != null) {
      var post = {
        _id: makeId(8),
        topicId: topicId,
        createdAt: obj.createdAt,
        author: obj.authorName || "Anonymous",
        text: obj.message,
        number: obj.number
      };
      if (obj.author) {
        post.userId = users[obj.author['$id'].toString()];
      }
      coll.insert(post);
    }
    nb ++;
  }
  coll.ensureIndex({topicId: 1});
  coll.ensureIndex({topicId: 1, createdAt: 1});
  print("Done posts: " + nb);
})(db.forum_post, db.f_post);

function makeId(size) {
  var text = "";
  var possible = "abcdefghijklmnopqrstuvwxyz0123456789";

  for( var i=0; i < size; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}
