[db.config, db.config_anon].forEach(function (coll) {
  coll.find().forEach(function (o) {
    var sets = {};
    var unsets = {};
    ['friend', 'hook', 'ai'].forEach(function (type) {
      if (!o[type]) return;
      sets[type + '.tm'] = o[type].k ? NumberInt(1) : NumberInt(0);
      sets[type + '.d'] = NumberInt(2);
      unsets[type + '.k'] = true;
    });
    coll.update(
      {
        _id: o._id,
      },
      {
        $set: sets,
        $unset: unsets,
      },
    );
  });
});
