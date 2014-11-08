var chessground = require('chessground');

function makeConfig(data, situation, flip) {
  return {
    viewOnly: true,
    fen: situation.fen,
    check: situation.check,
    lastMove: situation.lastMove,
    orientation: flip ? data.opponent.color : data.player.color,
    coordinates: data.pref.coords !== 0,
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      dragOver: false
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration
    },
    events: {
      capture: $.sound.take
    }
  };
}

function make(data, situation) {
  return new chessground.controller(makeConfig(data, situation));
}

module.exports = {
  make: make
};
