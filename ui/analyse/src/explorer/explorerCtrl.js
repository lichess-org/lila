var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;

module.exports = function(root, allow) {

  var storageKey = 'explorer-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var loading = m.prop(true);
  var config = {
    open: m.prop(true),
    db: {
      available: ['lichess', 'masters', 'me'],
      selected: m.prop('lichess')
    },
    rating: {
      available: [1600, 1800, 2000, 2200, 2500],
      selected: m.prop([2000, 2200, 2500])
    },
    speed: {
      available: ['bullet', 'blitz', 'classical'],
      selected: m.prop(['blitz', 'classical'])
    }
  };


  var cache = {};
  var clearCache = function() {
    cache = {};
    setStep();
  }

  var fetch = throttle(500, false, function() {
    var fen = root.vm.step.fen;
    m.request({
      method: 'GET',
      url: 'http://130.211.90.176/master?fen=' + fen
    }).then(function(data) {
      cache[fen] = data;
      loading(false);
    });
  });

  var empty = {
    moves: {}
  };

  function setStep() {
    if (!enabled()) return;
    var step = root.vm.step;
    if (step.ply > 40) cache[step.fen] = empty;
    if (!cache[step.fen]) {
      loading(true);
      fetch(step.fen);
    } else loading(false);
  }

  var toggleMany = function(c, value) {
    if (c().indexOf(value) === -1) c(c().concat([value]));
    else if (c().length > 1) c(c().filter(function(v) {
      return v !== value;
    }));
    m.redraw();
    clearCache();
  };

  return {
    allowed: allowed,
    enabled: enabled,
    setStep: setStep,
    loading: loading,
    config: config,
    current: function() {
      return cache[root.vm.step.fen];
    },
    toggle: function() {
      if (!allowed()) return;
      enabled(!enabled());
      m.redraw();
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
      setStep();
    },
    toggleConfig: function() {
      config.open(!config.open());
      m.redraw();
      return false;
    },
    toggleDb: function(db) {
      config.db.selected(db);
      m.redraw();
      clearCache();
    },
    toggleRating: partial(toggleMany, config.rating.selected),
    toggleSpeed: partial(toggleMany, config.speed.selected)
  };
};
