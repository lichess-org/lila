[db.config, db.config_anon].forEach(function(coll) {
  coll.find().forEach(function(o) {
    if (o.filter && o.filter.v && o.filter.v.length > 1) {
      coll.update({
        _id: o._id
      }, {
        $push: {
          'filter.v': NumberInt(8)
        }
      });
    };
  });
});
