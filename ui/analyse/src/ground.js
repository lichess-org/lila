var chessground = require('chessground');

function makeConfig(data, fen, flip) {
  return {
    viewOnly: true,
    fen: fen,
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

function make(data, fen) {
  return new chessground.controller(makeConfig(data, fen));
}

function reload(ground, data, fen, flip) {
  ground.set(makeConfig(data, fen, flip));
}

module.exports = {
  make: make,
  reload: reload
};
