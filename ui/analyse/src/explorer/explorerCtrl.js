var m = require('mithril');
var throttle = require('../util').throttle;
var configCtrl = require('./explorerConfig').controller;
var xhr = require('./openingXhr');
var storedProp = require('../util').storedProp;
var synthetic = require('../util').synthetic;
var replayable = require('game').game.replayable;

function tablebaseRelevant(fen) {
  var parts = fen.split(/\s/);
  var pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;
  return pieceCount <= 7;
}

module.exports = function(root, opts, allow) {

  var allowed = m.prop(allow);
  var enabled = root.embed ? m.prop(false) : storedProp('explorer.enabled', false);
  if (location.hash === '#opening' && !root.embed) enabled(true);
  var loading = m.prop(true);
  var failing = m.prop(false);
  var hoveringUci = m.prop(null);
  var movesAway = m.prop(0);

  var cache = {};
  var onConfigClose = function() {
    m.redraw();
    cache = {};
    setNode();
  }
  var withGames = synthetic(root.data) || replayable(root.data) || root.data.opponent.ai;
  var effectiveVariant = root.data.game.variant.key == 'fromPosition' ? 'standard' : root.data.game.variant.key;

  var config = configCtrl(root.data.game.variant, onConfigClose);

  var setCache = function(fen, res) {
    cache[fen] = res;
    movesAway(res.nbMoves ? 0 : movesAway() + 1);
  };

  var handleFetchError = function(err) {
    loading(false);
    failing(true);
    m.redraw();
  };

  var fetchOpening = throttle(250, function(fen) {
    xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames).then(function(res) {
      res.opening = true;
      res.nbMoves = res.moves.length;
      res.fen = fen;
      setCache(fen, res);
      loading(false);
      failing(false);
      m.redraw();
    }, handleFetchError);
  }, false);

  var fetchTablebase = throttle(250, function(fen) {
    xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, root.vm.node.fen).then(function(res) {
      res.nbMoves = res.moves.length;
      res.tablebase = true;
      res.fen = fen;
      setCache(fen, res);
      loading(false);
      failing(false);
      m.redraw();
    }, handleFetchError);
  }, false);

  var fetch = function(fen) {
    var hasTablebase = effectiveVariant === 'standard' || effectiveVariant === 'chess960' || effectiveVariant === 'atomic';
    if (hasTablebase && withGames && tablebaseRelevant(fen)) fetchTablebase(fen);
    else fetchOpening(fen);
  };

  var empty = {
    opening: true,
    moves: {}
  };

  function setNode() {
    if (!enabled()) return;
    var node = root.vm.node;
    if (node.ply > 50 && !tablebaseRelevant(node.fen)) {
      cache[node.fen] = empty;
    }
    var cached = cache[node.fen];
    if (cached) {
      movesAway(cached.nbMoves ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
    } else {
      loading(true);
      fetch(node.fen);
    }
  }

  return {
    allowed: allowed,
    enabled: enabled,
    setNode: setNode,
    loading: loading,
    failing: failing,
    hoveringUci: hoveringUci,
    movesAway: movesAway,
    config: config,
    withGames: withGames,
    current: function() {
      return cache[root.vm.node.fen];
    },
    toggle: function() {
      movesAway(0);
      enabled(!enabled());
      setNode();
      root.autoScroll();
    },
    disable: function() {
      if (enabled()) {
        enabled(false);
        root.autoScroll();
      }
    },
    setHoveringUci: function(uci) {
      hoveringUci(uci);
      root.setAutoShapes();
    }
  };
};
