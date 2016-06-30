var Chess = require('chess.js').Chess;
var util = require('./util');

module.exports = function(fen, appleKeys) {

  var chess = new Chess(fen);

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    var color = chess.turn() === 'w' ? 'b' : 'w';
    appleKeys.forEach(function(key) {
      chess.put({
        type: 'p',
        color: color
      }, key);
    });
  }

  function getColor() {
    return chess.turn() == "w" ? "white" : "black";
  }

  function setColor(c) {
    var turn = c === 'white' ? ' w ' : ' b ';
    var newFen = chess.fen().replace(/ (w|b) /, turn);
    chess.load(newFen);
    if (getColor() !== c) {
      // the en passant square prevents setting color
      newFen = newFen.replace(/ (w|b) - \w{2} /, turn + ' - - ');
      chess.load(newFen);
    }
  }

  return {
    dests: function() {
      var dests = {};
      chess.SQUARES.forEach(function(s) {
        var ms = chess.moves({
          square: s,
          verbose: true
        });
        if (ms.length) dests[s] = ms.map(function(m) {
          return m.to;
        });
      });
      return dests;
    },
    color: function(c) {
      if (c) setColor(c);
      else return getColor(c);
    },
    fen: chess.fen,
    move: function(orig, dest, prom) {
      return chess.move({
        from: orig,
        to: dest,
        promotion: prom ? util.roleToSan[prom].toLowerCase() : null
      });
    },
    occupation: function() {
      var map = {};
      chess.SQUARES.forEach(function(s) {
        var p = chess.get(s);
        if (p) map[s] = p;
      });
      return map;
    },
    findCapture: function() {
      var move = chess.moves({
        verbose: true
      }).filter(function(move) {
        return move.captured;
      })[0];
      if (move) return {
        orig: move.from,
        dest: move.to
      };
    },
    get: chess.get,
    undo: chess.undo,
    instance: chess
  };
};
