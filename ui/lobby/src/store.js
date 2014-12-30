var tab = {
  key: 'lichess.lobby.tab',
  fix: function(t) {
    if (['real_time', 'seeks', 'now_playing'].indexOf(t) === -1) t = 'real_time';
    return t;
  }
};
var mode = {
  key: 'lichess.lobby.mode',
  fix: function(m) {
    if (['list', 'chart'].indexOf(m) === -1) m = 'list';
    return m;
  }
};
var sort = {
  key: 'lichess.lobby.sort',
  fix: function(m) {
    if (['rating', 'time'].indexOf(m) === -1) m = 'rating';
    return m;
  }
};

function makeStore(conf) {
  return {
    set: function(t) {
      t = conf.fix(t);
      lichess.storage.set(conf.key, t);
      return t;
    },
    get: function() {
      return conf.fix(lichess.storage.get(conf.key));
    }
  };
}

module.exports = {
  tab: makeStore(tab),
  mode: makeStore(mode),
  sort: makeStore(sort)
};
