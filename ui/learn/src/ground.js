var chessground = require("./og/main");
var raf = chessground.util.requestAnimationFrame;
var util = require("./util");

var cg = new chessground.controller();

module.exports = {
  instance: cg,
  ab: "abcdef",
  set: function (opts) {
    var check = opts.shogi.instance.check;
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
      notation: document.getElementsByClassName("notation-0")[0] ? 0 : 1,
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
      check: checks ? checks[0].dest : null,
    });
    if (checks)
      cg.setShapes(
        checks.map(function (move) {
          return util.arrow(move.orig + move.dest, "yellow");
        })
      );
  },
  promote: function (key, role) {
    var pieces = {};
    var piece = cg.data.pieces[key];
    if (piece && piece.role === "pawn") {
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
  showCapture: function (move) {
    raf(function () {
      var $square = $("#learn-app piece[data-key=" + move.orig + "]");
      $square.addClass("wriggle");
      setTimeout(function () {
        $square.removeClass("wriggle");
        cg.setShapes([]);
        cg.apiMove(move.orig, move.dest);
      }, 600);
    });
  },
  showCheckmate: function (shogi) {
    //var turn = shogi.instance.player === "white" ? "b" : "w";
    //var fen = [cg.getFen(), turn, "- 1"].join(" ");
    //var s = shogi.instance.init(fen);
    //var kingKey = shogi.kingKey(turn === "w" ? "black" : "white");
    //var shapes = chess.instance
    //  .moves({
    //    verbose: true,
    //  })
    //  .filter(function (m) {
    //    return m.to === kingKey;
    //  })
    //  .map(function (m) {
    //    return util.arrow(m.from + m.to, "red");
    //  });
    //cg.set({
    //  check: shapes.length ? kingKey : null,
    //});
    //cg.setShapes(shapes);
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
      if (!!i[0] && !!i[1] && !!i[2])
        cg.apiNewPiece({ role: i[0], color: i[1] }, i[2]);
    }
    cg.data.lastMove = null;
  },
  select: cg.selectSquare,
};
