[db.config, db.config_anon].forEach(function(coll) {
  coll.update({
    'filter.v.1': {
      $exists: true
    }
  }, {
    $push: {
      'filter.v': NumberInt(9)
    }
  }, {
    multi: true
  });
});
