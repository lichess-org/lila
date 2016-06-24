var chessground = require('chessground');

var instance = new chessground.controller();

module.exports = {
  instance: instance,
  set: function(opts) {
    instance.set({
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
        enabled: true,
        duration: 300
      },
      disableContextMenu: true
    });
    if (opts.shapes) instance.setShapes(opts.shapes);
    return instance;
  },
  stop: instance.stop,
  color: function(color, dests) {
    instance.set({
      turnColor: color,
      movable: {
        color: color,
        dests: dests
      }
    });
  }
};
