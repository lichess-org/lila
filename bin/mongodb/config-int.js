function toInt(obj) {
  return function(prop) {
    if (typeof obj[prop] != 'undefined' && isNumber(obj[prop])) obj[prop] = NumberInt(obj[prop]);
  };
}

// db.config.find({_id:'thibault'}).forEach(function(config) {
[db.config, db.config_anon].forEach(function(coll) {
  coll.find().forEach(function(config) {
    // printjson(config);
    ['friend', 'hook', 'ai'].forEach(function(type) {
      ['d', 'i', 'm', 't', 'tm', 'v', 'l'].forEach(toInt(config[type]))
    });
    var filter = config.filter;
    ['m', 's', 'v'].forEach(function(prop) {
      if (typeof filter[prop] != 'undefined') filter[prop] = filter[prop].map(function(n) {
        return NumberInt(n);
      });
    });
    if (config.filter && config.filter.s) config.filter.s.push(NumberInt(99));
    // printjson(config);
    coll.update({
      "_id": config._id
    }, config);
  });
});
