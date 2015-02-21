var chessground = require('chessground');
var game = require('game').game;

function makeConfig(data, situation, onMove) {
  return {
    fen: situation.fen,
    check: situation.check,
    lastMove: situation.lastMove,
    orientation: data.player.color,
    coordinates: data.pref.coords !== 0,
    viewOnly: game.userAnalysableVariants.indexOf(data.game.variant.key) === -1,
    movable: {
      free: false,
      color: situation.movable.color,
      dests: situation.movable.dests,
      events: {
        after: onMove
      }
    },
    premovable: {
      enabled: false
    },
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      dragOver: true
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration
    },
    events: {
      capture: $.sound.take
    },
    disableContextMenu: true
  };
}

function make(data, situation, onMove) {
  return new chessground.controller(makeConfig(data, situation, onMove));
}

module.exports = {
  make: make
};
