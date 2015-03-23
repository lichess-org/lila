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
    events: {
      move: function(orig, dest, captured) {
        if (captured) $.sound.take();
      }
    },
    disableContextMenu: true
  };
}

function make(data, situation, onMove) {
  return new chessground.controller(makeConfig(data, situation, onMove));
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
