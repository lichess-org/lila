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

  var cacheKey = function() {
    if (config.data.db.selected() === 'watkins' && !tablebaseRelevant(effectiveVariant, root.vm.node.fen)) {
      var moves = [];
      for (var i = 1; i < root.vm.nodeList.length; i++) {
        moves.push(root.vm.nodeList[i].uci);
      }
      return moves.join(',');
    }
    else return root.vm.node.fen;
  };

  var handleFetchError = function(err) {
    loading(false);
    failing(true);
    m.redraw();
  };

  var fetch = throttle(250, function() {
    var fen = root.vm.node.fen, key = cacheKey();

    var request;
    if (withGames && tablebaseRelevant(effectiveVariant, fen))
      request = xhr.tablebase(opts.tablebaseEndpoint, effectiveVariant, fen);
    else if (config.data.db.selected() === 'watkins')
      request = xhr.watkins(opts.tablebaseEndpoint, key);
    else
      request = xhr.opening(opts.endpoint, effectiveVariant, fen, config.data, withGames);

    request.then(function(res) {
      res.nbMoves = res.moves.length;
      res.fen = root.vm.node.fen;
      cache[key] = res;
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
    if (node.ply > 50 && !tablebaseRelevant(effectiveVariant, node.fen)) {
      cache[node.fen] = empty;
    }
    var cached = cache[cacheKey()];
    if (cached) {
      movesAway(cached.nbMoves ? 0 : movesAway() + 1);
      loading(false);
      failing(false);
    } else {
      loading(true);
      fetch();
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
      return cache[cacheKey()];
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
