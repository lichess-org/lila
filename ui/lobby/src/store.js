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

module.exports = {
  tab: {
    set: function(t) {
      t = tab.fix(t);
      storage.set(tab.key, t);
      return t;
    },
    get: function() {
      return tab.fix(storage.get(tab.key));
    }
  },
  mode: {
    set: function(m) {
      m = mode.fix(m);
      storage.set(mode.key, m);
      return m;
    },
    get: function() {
      return mode.fix(storage.get(mode.key));
    }
  }
};
