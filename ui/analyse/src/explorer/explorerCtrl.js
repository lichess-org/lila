var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var configCtrl = require('./explorerConfig').controller;
var xhr = require('./explorerXhr');
var storedProp = require('../util').storedProp;

module.exports = function(root, endpoint) {

  var storageKey = 'explorer-enabled';
  var enabled = storedProp('explorer.enabled', false);
  var loading = m.prop(true);
  var failing = m.prop(false);
  var hoveringUci = m.prop(null);

  var cache = {};
  var clearCache = function() {
    cache = {};
    setStep();
  }

  var config = configCtrl(clearCache);

  var fetch = throttle(500, false, function() {
    var fen = root.vm.step.fen;
    xhr(endpoint, root.data.game.variant.key, fen, config.data).then(function(res) {
      cache[fen] = res;
      loading(false);
      failing(false);
      m.redraw();
    }, function(err) {
      loading(false);
      failing(true);
      m.redraw();
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

  return {
    enabled: enabled,
    setStep: setStep,
    loading: loading,
    failing: failing,
    hoveringUci: hoveringUci,
    config: config,
    current: function() {
      return cache[root.vm.step.fen];
    },
    toggle: function() {
      enabled(!enabled());
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
      setStep();
    },
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      root.setAutoShapes();
    }
  };
};
