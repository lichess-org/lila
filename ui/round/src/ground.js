var chessground = require('chessground');
var game = require('game').game;
var util = require('./util');
var m = require('mithril');

function str2move(mo) {
  return mo ? [mo.slice(0, 2), mo.slice(2, 4)] : null;
}

function makeConfig(data, fen, flip) {
  return {
    fen: fen,
    orientation: flip ? data.opponent.color : data.player.color,
    turnColor: data.game.player,
    lastMove: str2move(data.game.lastMove),
    check: data.game.check,
    coordinates: data.pref.coords !== 0,
    autoCastle: data.game.variant.key === 'standard',
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      dragOver: true
    },
    movable: {
      free: false,
      color: game.isPlayerPlaying(data) ? data.player.color : null,
      dests: util.parsePossibleMoves(data.possibleMoves),
      showDests: data.pref.destination
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration
    },
    premovable: {
      enabled: data.pref.enablePremove,
      showDests: data.pref.destination,
      castle: data.game.variant.key !== 'antichess',
      events: {
        set: m.redraw,
        unset: m.redraw
      }
    },
    draggable: {
      showGhost: data.pref.highlight
    },
    drawable: {
      enabled: true
    },
    disableContextMenu: true
  };
}

function make(data, fen, userMove, onMove) {
  var config = makeConfig(data, fen);
  config.movable.events = {
    after: userMove
  };
  config.events = {
    move: onMove
  };
  config.viewOnly = data.player.spectator;
  return new chessground.controller(config);
}

function reload(ground, data, fen, flip) {
  ground.set(makeConfig(data, fen, flip));
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

function end(ground) {
  ground.stop();
}

module.exports = {
  make: make,
  reload: reload,
  promote: promote,
  end: end
};
