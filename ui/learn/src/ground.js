var chessground = require('chessground');

module.exports = {
  make: function(opts) {
    return new chessground.controller({
      fen: opts.chess.fen(),
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
        enabled: true,
        duration: 300
      },
      disableContextMenu: true
    });
  },
  color: function(ground, color, dests) {
    ground.set({
      turnColor: color,
      movable: {
        color: color,
        dests: dests
      }
    });
  }
};
