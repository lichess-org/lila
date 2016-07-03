var chessground = require('chessground');
var partial = chessground.util.partial;
var raf = chessground.util.requestAnimationFrame;

var cg = new chessground.controller();

module.exports = {
  instance: cg,
  set: function(opts) {
    var check = opts.chess.instance.in_check();
    cg.set({
      fen: opts.chess.fen(),
      lastMove: null,
      orientation: opts.orientation,
      coordinates: true,
      squareKey: true,
      turnColor: opts.chess.color(),
      check: check,
      movable: {
        free: false,
        color: opts.chess.color(),
        dests: opts.chess.dests()
      },
      events: {
        move: opts.onMove
      },
      items: opts.items,
      premovable: {
        enabled: true
      },
      drawable: {
        enabled: true,
        eraseOnClick: true
      },
      highlight: {
        lastMove: true,
        dragOver: true
      },
      animation: {
        enabled: false, // prevent piece animation during transition
        duration: 200
      },
      disableContextMenu: true
    });
    setTimeout(function() {
      cg.set({
        animation: {
          enabled: true
        }
      });
    }, 200);
    if (opts.shapes) cg.setShapes(opts.shapes);
    return cg;
  },
  stop: cg.stop,
  color: function(color, dests) {
    cg.set({
      turnColor: color,
      movable: {
        color: color,
        dests: dests
      }
    });
  },
  fen: function(fen, color, dests) {
    cg.set({
      turnColor: color,
      fen: fen,
      movable: {
        color: color,
        dests: dests
      }
    });
  },
  check: function(chess) {
    var checks = chess.checks();
    cg.set({
      check: !!checks
    });
    if (checks) cg.setShapes(checks.map(function(move) {
      return {
        brush: 'yellow',
        orig: move.orig,
        dest: move.dest
      };
    }));
  },
  promote: function(key, role) {
    var pieces = {};
    var piece = cg.data.pieces[key];
    if (piece && piece.role === 'pawn') {
      pieces[key] = {
        color: piece.color,
        role: role
      };
      cg.setPieces(pieces);
    }
  },
  data: function() {
    return cg.data;
  },
  pieces: function() {
    return cg.data.pieces;
  },
  get: function(key) {
    return cg.data.pieces[key];
  },
  showCapture: function(move) {
    raf(function() {
      var $square = $('#learn_app square[data-key=' + move.orig + ']');
      $square.addClass('wriggle');
      setTimeout(function() {
        $square.removeClass('wriggle');
        cg.setShapes([]);
        cg.apiMove(move.orig, move.dest);
      }, 600);
    });
    // var shapes = [{
    //   brush: 'red',
    //   orig: move.orig,
    //   dest: move.dest
    // }];
    // for (var i = 0; i < 4; i++) {
    //   setTimeout(partial(cg.setShapes, shapes), i * 300);
    //   setTimeout(partial(cg.setShapes, []), i * 300 + 150);
    // }
  },
  resetShapes: function() {
    cg.setShapes([]);
  }
};
