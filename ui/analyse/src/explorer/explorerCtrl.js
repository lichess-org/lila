var m = require('mithril');
var throttle = require('common').throttle;
var configCtrl = require('./explorerConfig').controller;
var xhr = require('./openingXhr');
var storedProp = require('common').storedProp;
var synthetic = require('../util').synthetic;
var replayable = require('game').game.replayable;

function tablebaseRelevant(variant, fen) {
  var parts = fen.split(/\s/);
  var pieceCount = parts[0].split(/[nbrqkp]/i).length - 1;

  if (variant === 'standard' || variant === 'chess960' || variant === 'atomic')
    return pieceCount <= 7;
  else if (variant === 'antichess') return pieceCount <= 6;
  else return false;
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

  var handleFetchError = function(err) {
    loading(false);
    failing(true);
    m.redraw();
  };

  var fetchOpening = function(fen) {
    return xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames);
  };

  var fetchTablebase = function(fen) {
    return xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen);
  };

  var cacheResult = function(fen, res, isTablebase) {
      res[isTablebase ? 'tablebase' : 'opening'] = true;
      res.nbMoves = res.moves.length;
      res.fen = fen;
    cache[fen] = res;
  };

  var fetch = throttle(250, function(fen) {
    var isTablebase = withGames && tablebaseRelevant(effectiveVariant, fen);
    (isTablebase ? fetchTablebase : fetchOpening)(fen).then(function(res) {
      cacheResult(fen, res, isTablebase);
      movesAway(res.nbMoves ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
      m.redraw();
    }, handleFetchError);
  }, false);

  var empty = {
    opening: true,
    moves: {}
  };

  function setNode() {
    if (!enabled()) return;
    var node = root.vm.node;
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.fen)) {
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
    },
    fetchOpening: function(fen) {
      if (cache[fen]) {
        var d = m.deferred(cache[fen]);
        setTimeout(function() {
          d.resolve(cache[fen]);
        }, 10);
        return d.promise;
      }
      return fetchOpening(fen).then(function(res) {
        cacheResult(fen, res, false);
        return res;
      });
    }
  };
};
