var props = ['animation', 'autoQueen', 'autoThreefold', 'challenge', 'takeback'];
db.pref.find().forEach(function (p) {
  var set = {},
    unset = {};
  props.forEach(function (prop) {
    if (typeof p[prop] !== 'undefined') {
      unset[prop] = true;
      set[prop] = new NumberInt(p[prop]);
    }
  });
  // must unset first, or update does not happen D:
  db.pref.update(
    {
      _id: p._id,
    },
    {
      $unset: set,
    },
  );
  db.pref.update(
    {
      _id: p._id,
    },
    {
      $set: set,
    },
  );
});

print('Done!');
