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
  var hovering = m.prop(null);
  var movesAway = m.prop(0);

  var cache = {};
  var onConfigClose = function() {
    m.redraw();
    cache = {};
    setNode();
  }
  var withGames = synthetic(root.data) || replayable(root.data) || root.data.opponent.ai;
  var effectiveVariant = root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;

  var config = configCtrl(root.data.game, withGames, onConfigClose);

  var handleFetchError = function(err) {
    loading(false);
    failing(true);
    m.redraw();
  };

  var fetchOpening = function(fen) {
    if (config.data.db.selected() === 'watkins') return fetchWatkins(opts.tablebaseEndpoint);
    else return xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames);
  };

  var fetchTablebase = function(fen) {
    return xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen);
  };

  var fetchWatkins = function(fen) {
    var moves = [];
    for (var i = 1; i < root.vm.nodeList.length; i++) {
      moves.push(root.vm.nodeList[i].uci);
    }
    return xhr.watkins(opts.tablebaseEndpoint, moves);
  };

  var cacheResult = function(fen, res) {
    res.nbMoves = res.moves.length;
    res.fen = fen;
    cache[fen] = res;
  };

  var fetch = throttle(250, function(fen) {
    var isTablebase = withGames && tablebaseRelevant(effectiveVariant, fen);
    (isTablebase ? fetchTablebase : fetchOpening)(fen).then(function(res) {
      cacheResult(fen, res);
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

  var setNode = function() {
    if (!enabled()) return;
    var node = root.vm.node;
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.fen) && config.data.db.selected() !== 'watkins') {
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
  };

  return {
    allowed: allowed,
    enabled: enabled,
    setNode: setNode,
    loading: loading,
    failing: failing,
    hovering: hovering,
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
    setHovering: function(fen, uci) {
      hovering(uci ? {
        fen: fen,
        uci: uci,
      } : null);
      root.setAutoShapes();
    },
    fetchMasterOpening: (function() {
      var masterCache = {};
      return function(fen) {
        if (masterCache[fen]) {
          var d = m.deferred();
          d.resolve(masterCache[fen]);
          return d.promise;
        }
        return xhr.opening(opts.endpoint, 'standard', fen, {
          db: {
            selected: m.prop('masters')
          }
        }, false).then(function(res) {
          masterCache[fen] = res;
          return res;
        });
      }
    })()
  };
};
