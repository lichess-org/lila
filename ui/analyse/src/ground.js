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
  var c = new chessground.controller(makeConfig(data, config, onMove));
  c.data.drawable.brushes.paleBlueSmall = {
    key: 'pbs',
    color: '#003088',
    opacity: 0.45,
    lineWidth: 8,
    circleMargin: 0
  };
  return c;
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
