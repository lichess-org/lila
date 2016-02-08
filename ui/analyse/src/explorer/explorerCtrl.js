var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var configCtrl = require('./explorerConfig').controller;
var xhr = require('./explorerXhr');

module.exports = function(root, allow) {

  var storageKey = 'explorer-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var loading = m.prop(true);

  var cache = {};
  var clearCache = function() {
    cache = {};
    setStep();
  }

  var config = configCtrl(clearCache);

  var fetch = throttle(500, false, function() {
    var fen = root.vm.step.fen;
    xhr(root.data.game.variant.key, fen, config.data).then(function(res) {
      cache[fen] = res;
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
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
      setStep();
    },
  };
};
