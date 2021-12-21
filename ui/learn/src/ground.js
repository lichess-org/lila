const { opposite } = require('shogiops');
const { makeChessSquare } = require('shogiops/compat');
var squareSet = require('shogiops/squareSet');
var chessground = require('./og/main');
var raf = chessground.util.requestAnimationFrame;
var util = require('./util');
const timeouts = require('./timeouts');

var cg = new chessground.controller();

module.exports = {
  instance: cg,
  set: function (opts) {
    var check = opts.shogi.isCheck();
    cg.set({
      fen: opts.shogi.fen(),
      lastMove: opts.lastMoves,
      selected: null,
      orientation: opts.orientation,
      coordinates: true,
      pieceKey: true,
      turnColor: opts.shogi.color(),
      check: check,
      autoCastle: opts.autoCastle,
      movable: {
        free: false,
        color: opts.shogi.color(),
        dests: opts.shogi.dests({
          illegal: opts.offerIllegalMove,
        }),
      },
      events: {
        move: opts.onMove,
        dropNewPiece: opts.onDrop,
      },
      items: opts.items,
      premovable: {
        enabled: true,
      },
      drawable: {
        enabled: true,
        eraseOnClick: true,
      },
      highlight: {
        lastMove: true,
        dragOver: true,
      },
      animation: {
        enabled: false, // prevent piece animation during transition
        duration: 200,
      },
      disableContextMenu: true,
      notation: document.getElementsByClassName('notation-0')[0] ? 0 : 1,
      dropmode: {
        showDropDests: opts.dropmode.showDropDests,
        dropDests: opts.dropmode.dropDests,
      },
    });
    setTimeout(function () {
      cg.set({
        animation: {
          enabled: true,
        },
      });
    }, 200);
    if (opts.shapes) cg.setShapes(opts.shapes.slice(0));
    return cg;
  },
  stop: cg.stop,
  color: function (color, dests) {
    cg.set({
      turnColor: color,
      movable: {
        color: color,
        dests: dests,
      },
    });
  },
  fen: function (fen, color, dests, lastMove) {
    var config = {
      turnColor: color,
      fen: fen,
      movable: {
        color: color,
        dests: dests,
      },
    };
    if (lastMove) config.lastMove = lastMove;
    cg.set(config);
  },
  check: function (shogi) {
    var checks = shogi.checks();
    cg.set({
      check: checks && checks[0] ? checks[0].dest : null,
    });
    if (checks)
      cg.setShapes(
        checks.map(function (move) {
          return util.arrow(move.orig + move.dest, 'yellow');
        })
      );
  },
  promote: function (key, role) {
    var pieces = {};
    var piece = cg.data.pieces[key];
    if (piece) {
      pieces[key] = {
        color: piece.color,
        role: role,
        promoted: true,
      };
      cg.setPieces(pieces);
    }
  },
  data: function () {
    return cg.data;
  },
  pieces: function () {
    return cg.data.pieces;
  },
  get: function (key) {
    return cg.data.pieces[key];
  },
  showCapture: function (move, shogiopsMove, m) {
    raf(function () {
      var $square = $('#learn-app piece[data-key=' + move.orig + ']');
      $square.addClass('wriggle');
      timeouts.setTimeout(function () {
        $square.removeClass('wriggle');
        cg.setShapes([]);
        cg.apiMove(move.orig, move.dest);
        if (shogiopsMove && m) {
          shogiopsMove();
          m.redraw();
        }
      }, 600);
    });
  },
  showNifu: function (squares) {
    cg.setShapes(
      squares.map(function (square) {
        return util.circle(square, 'red');
      })
    );
  },
  showCheckmate: function (shogi) {
    const kingSquare = shogi.instance.board.kingOf(opposite(shogi.color()));
    const allDests = shogi.instance.allDests({
      king: undefined,
      blockers: squareSet.SquareSet.empty(),
      checkers: squareSet.SquareSet.empty(),
      variantEnd: false,
      mustCapture: false,
    });
    var attacksOnKing = [];
    for (let m of allDests.keys()) {
      if (allDests.get(m).has(kingSquare)) attacksOnKing.push(m);
    }
    const shapes = attacksOnKing.map(function (m) {
      return util.arrow(makeChessSquare(m) + makeChessSquare(kingSquare), 'red');
    });
    cg.set({
      check: shapes.length ? makeChessSquare(kingSquare) : null,
    });
    cg.setShapes(shapes);
  },
  setShapes: function (shapes) {
    if (shapes) cg.setShapes(shapes);
  },
  resetShapes: function () {
    cg.setShapes([]);
  },
  lastMoves: function (moves) {
    cg.data.lastMove = moves;
  },
  newPieces: function (arr) {
    if (!arr) return;
    for (var i of arr) {
      if (!!i[0] && !!i[1] && !!i[2]) cg.apiNewPiece({ role: i[0], color: i[1] }, i[2]);
    }
    cg.data.lastMove = null;
  },
  select: cg.selectSquare,
};
