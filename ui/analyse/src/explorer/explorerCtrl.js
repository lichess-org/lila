var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var configCtrl = require('./explorerConfig').controller;
var xhr = require('./explorerXhr');
var storedProp = require('../util').storedProp;
var synthetic = require('../util').synthetic;
var replayable = require('game').game.replayable;

module.exports = function(root, opts) {

  var enabled = storedProp('explorer.enabled', false);
  var loading = m.prop(true);
  var failing = m.prop(false);
  var hoveringUci = m.prop(null);

  var cache = {};
  var onConfigClose = function() {
    m.redraw();
    cache = {};
    setStep();
  }
  var withGames = synthetic(root.data) || replayable(root.data) || root.data.opponent.ai;

  var config = configCtrl(root.data.game.variant, onConfigClose);

  var fetch = throttle(500, false, function() {
    var fen = root.vm.step.fen;
    xhr(opts.endpoint, root.data.game.variant.key, fen, config.data, withGames).then(function(res) {
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
    if (!opts.authorized || !enabled()) return;
    var step = root.vm.step;
    if (step.ply > 50) cache[step.fen] = empty;
    if (!cache[step.fen]) {
      loading(true);
      fetch(step.fen);
    } else loading(false);
  }

  return {
    authorized: opts.authorized,
    enabled: enabled,
    setStep: setStep,
    loading: loading,
    failing: failing,
    hoveringUci: hoveringUci,
    config: config,
    withGames: withGames,
    current: function() {
      return cache[root.vm.step.fen];
    },
    toggle: function() {
      enabled(!enabled());
      setStep();
      root.autoScroll();
    },
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      root.setAutoShapes();
    }
  };
};
