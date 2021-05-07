var Chess = require('chess.js').Chess;
var util = require('./util');

module.exports = function (fen, appleKeys) {
  var chess = new Chess(fen);

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    var color = chess.turn() === 'w' ? 'b' : 'w';
    appleKeys.forEach(function (key) {
      chess.put(
        {
          type: 'p',
          color: color,
        },
        key
      );
    });
  }

  function getColor() {
    return chess.turn() == 'w' ? 'white' : 'black';
  }

  function setColor(c) {
    var turn = c === 'white' ? 'w' : 'b';
    var newFen = util.setFenTurn(chess.fen(), turn);
    chess.load(newFen);
    if (getColor() !== c) {
      // the en passant square prevents setting color
      newFen = newFen.replace(/ (w|b) ([kKqQ-]{1,4}) \w\d /, ' ' + turn + ' $2 - ');
      chess.load(newFen);
    }
  }

  var findCaptures = function () {
    return chess
      .moves({
        verbose: true,
      })
      .filter(function (move) {
        return move.captured;
      })
      .map(function (move) {
        return {
          orig: move.from,
          dest: move.to,
        };
      });
  };

  return {
    dests: function (opts) {
      opts = opts || {};
      var dests = {};
      chess.SQUARES.forEach(function (s) {
        var ms = chess.moves({
          square: s,
          verbose: true,
          legal: !opts.illegal,
        });
        if (ms.length)
          dests[s] = ms.map(function (m) {
            return m.to;
          });
      });
      return dests;
    },
    color: function (c) {
      if (c) setColor(c);
      else return getColor();
    },
    fen: chess.fen,
    move: function (orig, dest, prom) {
      return chess.move({
        from: orig,
        to: dest,
        promotion: prom ? util.roleToSan[prom].toLowerCase() : null,
      });
    },
    occupation: function () {
      var map = {};
      chess.SQUARES.forEach(function (s) {
        var p = chess.get(s);
        if (p) map[s] = p;
      });
      return map;
    },
    kingKey: function (color) {
      for (var i in chess.SQUARES) {
        var p = chess.get(chess.SQUARES[i]);
        if (p && p.type === 'k' && p.color === (color === 'white' ? 'w' : 'b')) return chess.SQUARES[i];
      }
    },
    findCapture: function () {
      return findCaptures()[0];
    },
    findUnprotectedCapture: function () {
      return findCaptures().find(function (capture) {
        var clone = new Chess(chess.fen());
        clone.move({ from: capture.orig, to: capture.dest });
        return !clone
          .moves({
            verbose: true,
          })
          .some(function (m) {
            return m.captured && m.to === capture.dest;
          });
      });
    },
    checks: function () {
      if (!chess.in_check()) return null;
      var color = getColor();
      setColor(color === 'white' ? 'black' : 'white');
      var checks = chess
        .moves({
          verbose: true,
        })
        .filter(function (move) {
          return move.captured === 'k';
        })
        .map(function (move) {
          return {
            orig: move.from,
            dest: move.to,
          };
        });
      setColor(color);
      return checks;
    },
    playRandomMove: function () {
      var moves = chess.moves({
        verbose: true,
      });
      if (moves.length) {
        var move = moves[Math.floor(Math.random() * moves.length)];
        chess.move(move);
        return {
          orig: move.from,
          dest: move.to,
        };
      }
    },
    get: chess.get,
    undo: chess.undo,
    instance: chess,
  };
};
