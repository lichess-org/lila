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
    setNode();
  }
  var withGames = synthetic(root.data) || replayable(root.data) || root.data.opponent.ai;

  var config = configCtrl(root.data.game.variant, onConfigClose);

  var fetch = throttle(500, false, function() {
    var fen = root.vm.node.fen;
    var effectiveVariant = root.data.game.variant.key == 'fromPosition' ? 'standard' : root.data.game.variant.key;
    xhr(opts.endpoint, effectiveVariant, fen, config.data, withGames).then(function(res) {
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

  function setNode() {
    if (!enabled()) return;
    var node = root.vm.node;
    if (node.ply > 50) cache[node.fen] = empty;
    if (!cache[node.fen]) {
      loading(true);
      fetch(node.fen);
    } else loading(false);
  }

  return {
    enabled: enabled,
    setNode: setNode,
    loading: loading,
    failing: failing,
    hoveringUci: hoveringUci,
    config: config,
    withGames: withGames,
    current: function() {
      return cache[root.vm.node.fen];
    },
    toggle: function() {
      enabled(!enabled());
      setNode();
      root.autoScroll();
    },
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      root.setAutoShapes();
    }
  };
};
