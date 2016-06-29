var chessground = require('chessground');

var cg = new chessground.controller();

module.exports = {
  instance: cg,
  set: function(opts) {
    cg.set({
      fen: opts.chess.fen(),
      lastMove: null,
      orientation: opts.orientation,
      coordinates: true,
      turnColor: opts.chess.color(),
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
  }
};
