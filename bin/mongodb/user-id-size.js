var coll = db.user4;

print("Migrating " + coll.count() + " users");

coll.find({}, {
  _id: 1
}).forEach(function(u) {
  coll.update({
    _id: u._id
  }, {
    $set: {
      len: NumberInt(u._id.length)
    }
  });
});
