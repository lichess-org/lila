var chessground = require('chessground');
var round = require('./round');
var util = require('./util');

function makeConfig(data, fen) {
  return {
    fen: fen,
    orientation: data.player.color,
    turnColor: data.game.player,
    lastMove: util.str2move(data.game.lastMove),
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      dragOver: true
    },
    movable: {
      free: false,
      color: round.isPlayerPlaying(data) ? data.player.color : null,
      dests: round.parsePossibleMoves(data.possibleMoves),
      showDests: data.pref.destination
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration
    },
    premovable: {
      enabled: data.pref.enablePremove,
      showDests: data.pref.destination
    }
  };
}

function make(data, fen, userMove) {
  var config = makeConfig(data, fen);
  config.movable.events = {
    after: userMove
  };
  return new chessground.controller(config);
}

function reload(ground, data, fen) {
  ground.set(makeConfig(data, fen));
}

function end(ground) {
  ground.stop();
}

module.exports = {
  make: make,
  reload: reload,
  end: end
};
