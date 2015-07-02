var chessground = require('chessground');
var game = require('game').game;

function makeConfig(data, config, onMove) {
  return {
    fen: config.fen,
    check: config.check,
    lastMove: config.lastMove,
    orientation: data.orientation,
    coordinates: data.pref.coords !== 0,
    movable: {
      free: false,
      color: config.movable.color,
      dests: config.movable.dests
    },
    events: {
      move: onMove
    },
    premovable: {
      enabled: true
    },
    drawable: {
      enabled: true
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
    disableContextMenu: true
  };
}

function make(data, config, onMove) {
  return new chessground.controller(makeConfig(data, config, onMove));
}

function promote(ground, key, role) {
  var pieces = {};
  var piece = ground.data.pieces[key];
  if (piece && piece.role == 'pawn') {
    pieces[key] = {
      color: piece.color,
      role: role
    };
    ground.setPieces(pieces);
  }
}

module.exports = {
  make: make,
  promote: promote
};
