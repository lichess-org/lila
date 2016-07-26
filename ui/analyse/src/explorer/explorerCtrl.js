var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var configCtrl = require('./explorerConfig').controller;
var openingXhr = require('./openingXhr');
var tablebaseXhr = require('./tablebaseXhr');
var storedProp = require('../util').storedProp;
var synthetic = require('../util').synthetic;
var replayable = require('game').game.replayable;

function tablebaseRelevant(fen) {
  var parts = fen.split(/\s/);
  var pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;
  var castling = parts[2];
  return pieceCount <= 6 && castling === '-';
}

module.exports = function(root, opts, allow) {

  var allowed = m.prop(allow);
  var enabled = storedProp('explorer.enabled', false);
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

  var fetchOpening = throttle(2000, false, function(fen) {
    openingXhr(opts.endpoint, effectiveVariant, fen, config.data, withGames).then(function(res) {
      res.opening = true;
      res.nbMoves = res.moves.length;
      res.fen = fen;
      setCache(fen, res);
      loading(false);
      failing(false);
      m.redraw();
    }, handleFetchError);
  });

  var fetchTablebase = throttle(500, false, function(fen) {
    tablebaseXhr(opts.tablebaseEndpoint, root.vm.node.fen).then(function(res) {
      res.nbMoves = res.moves.length;
      res.tablebase = true;
      res.fen = fen;
      setCache(fen, res);
      loading(false);
      failing(false);
      m.redraw();
    }, handleFetchError);
  });

  var fetch = function(fen) {
    var hasTablebase = effectiveVariant === 'standard' || effectiveVariant === 'chess960';
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
    console.log(node);
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
    console.log(cached);
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
