var chessground = require('chessground');

function makeConfig(data, config, onMove) {
  return {
    fen: config.fen,
    check: config.check,
    lastMove: config.lastMove,
    orientation: data.puzzle.color,
    coordinates: data.pref.coords !== 0,
    movable: {
      free: false,
      color: config.movable.color,
      dests: config.movable.dests,
      rookCastle: data.pref.rookCastle
    },
    events: {
      move: onMove
    },
    premovable: {
      enabled: config.movable.enabled
    },
    drawable: {
      enabled: true
    },
    highlight: {
      lastMove: true,
      check: true,
      dragOver: true
    },
    animation: {
      enabled: true,
      duration: data.animation.duration
    },
    disableContextMenu: true
  };
}

module.exports = function(data, config, onMove) {
  return new chessground.controller(makeConfig(data, config, onMove));
};
