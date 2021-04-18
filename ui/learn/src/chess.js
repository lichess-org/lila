var Shogi = require("shogiops").Shogi;
var util = require("shogiops/util");
var fenUtil = require("shogiops/fen");
var compat = require("shogiops/compat");
var squareSet = require("shogiops/squareSet");


module.exports = function (fen, appleKeys) {
  if(fen.split(' ').length === 1) fen += ' b'
  var shogi = Shogi.fromSetup(fenUtil.parseFen(fen).unwrap(), false).unwrap();

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    appleKeys.forEach(function (key) {
      shogi.board.set(compat.parseChessSquare(key), {role: 'pawn', color: util.opposite(shogi.turn)});
    });
  }

  function placePiece(role, color, key) {
    shogi.board.set(compat.parseChessSquare(key), {role: role, color: color});
  }

  function getColor() {
    return shogi.turn;
  }

  function setColor(c) {
    shogi.turn = c;
  }

  function findKing(c) {
    return compat.makeChessSquare(shogi.board.kingOf(util.opposite(c)));
  }

  var findCaptures = function () {
    var allCaptures = [];
    for (const [o, d] of shogi.allDests()) {
      for (const s of d){
        if(shogi.board[util.opposite(shogi.turn)].has(s))
          allCaptures.push({ orig: compat.makeChessSquare(o), dest: compat.makeChessSquare(s) });
        }
      }
    return allCaptures;
  };

  // This might be moved to shogiops later
  var illegalMoves = function () {
    const result = new Map();
    const illegalDests = shogi.allDests({
      king: undefined,
      blockers: squareSet.SquareSet.empty(),
      checkers: squareSet.SquareSet.empty(),
      variantEnd: false,
      mustCapture: false,
    });
    for(const [from, squares] of illegalDests){
      if(squares.nonEmpty()){
        const d = Array.from(squares, s => compat.makeChessSquare(s));
        result.set(compat.makeChessSquare(from), d);
      }
    }
    return result;
  }

  return {
    dests: function (opts) {
      opts = opts || {};
      if (opts.illegal) return Object.fromEntries(illegalMoves());
      return Object.fromEntries(compat.shogigroundDests(shogi));
    },
    pockets: function () {
      return shogi.pockets;
    },
    color: function (c) {
      if (c) setColor(c);
      else return getColor();
    },
    fen: function () {
      return fenUtil.makeFen(shogi.toSetup());
    },
    move: function (orig, dest, prom) {
      shogi.play({from: compat.parseChessSquare(orig), to: compat.parseChessSquare(dest), promotion: prom});
      return { from: orig, to: dest, promotion: prom };
    },
    occupation: function () {
      return shogi.board;
    },
    kingKey: function (color) {
      return findKing(color);
    },
    findCapture: function () {
      return findCaptures()[0];
    },
    findUnprotectedCapture: function () {
      return findCaptures().find(function(capture) {
        const clone = shogi.clone();
        clone.play({from: capture.from, to: capture.to, promotion: capture.promotion});
        for(const [_, d] of clone.allDests()){
          if(d.has(capture.to)) return false;
        }
        return true;
      });
    },
    isCheck: function () {
      const clone = shogi.clone();
      clone.turn = util.opposite(clone.turn);
      if(shogi.isCheck() || clone.isCheck()) return true;
      return false;
    },
    checks: function () {
      const clone = shogi.clone();
      clone.turn = util.opposite(clone.turn);
      const colorInCheck = shogi.isCheck() ? shogi.turn : clone.isCheck() ? clone.turn : undefined;
      if (colorInCheck === undefined) return null;
      const kingPos = this.kingKey(util.opposite(colorInCheck));

      setColor(colorInCheck);
      const allDests = shogi.allDests();
      const origOfCheck = [];
      for(const k of allDests.keys()){
        if(allDests.get(k).has(compat.parseChessSquare(kingPos)))
          origOfCheck.push(k);
      }
      const checks = origOfCheck.map(s => {
        return {
          orig: compat.makeChessSquare(s),
          dest: kingPos
        }
      })
      setColor(util.opposite(colorInCheck));
      return checks;
    },
    playRandomMove: function () {
      const allD = shogi.allDests();
      const keys = Array.from(allD.keys());
      const from = keys[Math.floor(Math.random() * keys.length)];
      // first() is not really random but good enough
      const to = allD.get(from).first();
      shogi.play({from: from, to: to});
      return { orig: from, dest: to };
    },
    place: placePiece,
    instance: shogi,
  };
};
