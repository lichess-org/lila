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
  return pieceCount <= 7;
}

function tablebasePrepare(moves, fen) {
  var fenParts = fen.split(/\s/);
  var stm = fenParts[1];
  var halfMoves = parseInt(fenParts[4], 10);

  var result = [];

  for (uci in moves) {
    if (moves.hasOwnProperty(uci)) {
      var move = moves[uci];
      if (move.dtz === null) continue;

      move.uci = uci;

      if ((move.dtz < 0 && stm === 'w') || (move.dtz > 0 && stm === 'b')) {
        move.winner = 'white';
      } else if ((move.dtz < 0 && stm === 'b') || (move.dtz > 0 && stm === 'w')) {
        move.winner = 'black';
      }

      if (move.wdl == -2 && move.dtz - halfMoves <= -100) move.realWdl = -1
      else if (move.wdl == 2 && move.dtz + halfMoves >= 100) move.realWdl = 1
      else move.realWdl = move.wdl;

      result.push(move);
    }
  }

  result.sort(function (a, b) {
    if (a.dtm !== null && b.dtm !== null && a.dtm !== b.dtm) return b.dtm - a.dtm;
    return b.dtz - a.dtz;
  });

  return result;
}

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
    if (effectiveVariant === 'standard' && tablebaseRelevant(fen)) {
      tablebaseXhr(fen).then(function(res) {
        cache[fen] = {
          tablebase: true,
          fen: fen,
          moves: tablebasePrepare(res.moves, fen)
        };
        loading(false);
        failing(false);
        m.redraw();
      }, function (err) {
        loading(false);
        failing(true);
        m.redraw();
      });
    } else {
      openingXhr(opts.endpoint, effectiveVariant, fen, config.data, withGames).then(function(res) {
        res.opening = true;
        cache[fen] = res;
        loading(false);
        failing(false);
        m.redraw();
      }, function(err) {
        loading(false);
        failing(true);
        m.redraw();
      });
    }
  });

  var empty = {
    moves: {}
  };

  function setNode() {
    if (!enabled()) return;
    var node = root.vm.node;
    if (node.ply > 50 && !tablebaseRelevant(node.fen)) cache[node.fen] = empty;
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
