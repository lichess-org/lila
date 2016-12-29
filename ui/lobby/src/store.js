var tab = {
  key: 'lobby.tab',
  fix: function(t) {
    if (['pools', 'real_time', 'seeks', 'now_playing'].indexOf(t) === -1) t = 'pools';
    return t;
  }
};
var mode = {
  key: 'lobby.mode',
  fix: function(m) {
    if (['list', 'chart'].indexOf(m) === -1) m = 'list';
    return m;
  }
};
var sort = {
  key: 'lobby.sort',
  fix: function(m) {
    if (['rating', 'time'].indexOf(m) === -1) m = 'rating';
    return m;
  }
};

function makeStore(conf, userId) {
  var fullKey = conf.key + ':' + (userId || '-');
  return {
    set: function(t) {
      t = conf.fix(t);
      lichess.storage.set(fullKey, t);
      return t;
    },
    get: function() {
      return conf.fix(lichess.storage.get(fullKey));
    }
  };
}

module.exports = function(userId) {
  return {
    tab: makeStore(tab, userId),
    mode: makeStore(mode, userId),
    sort: makeStore(sort, userId)
  }
};
