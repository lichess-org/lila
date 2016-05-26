var chessground = require('chessground');
var game = require('game').game;
var util = require('./util');
var round = require('./round');
var m = require('mithril');

function str2move(mo) {
  return mo ? [mo.slice(0, 2), mo.slice(2, 4)] : null;
}

function boardOrientation(data, flip) {
  if (data.game.variant.key === 'racingKings') {
    return flip ? 'black': 'white';
  } else {
    return flip ? data.opponent.color : data.player.color;
  }
}

function makeConfig(data, ply, flip) {
  var step = round.plyStep(data, ply);
  var playing = game.isPlayerPlaying(data);
  return {
    fen: step.fen,
    orientation: boardOrientation(data, flip),
    turnColor: data.game.player,
    lastMove: str2move(step.uci),
    check: step.check,
    coordinates: data.pref.coords !== 0,
    autoCastle: data.game.variant.key === 'standard',
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      dragOver: true
    },
    movable: {
      free: false,
      color: playing ? data.player.color : null,
      dests: playing ? util.parsePossibleMoves(data.possibleMoves) : {},
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
    predroppable: {
      enabled: data.pref.enablePremove && data.game.variant.key === 'crazyhouse',
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

function make(data, ply, userMove, userNewPiece, onMove, onNewPiece) {
  var config = makeConfig(data, ply);
  config.movable.events = {
    after: userMove,
    afterNewPiece: userNewPiece
  };
  config.events = {
    move: onMove,
    dropNewPiece: onNewPiece
  };
  config.viewOnly = data.player.spectator;
  return new chessground.controller(config);
}

function reload(ground, data, ply, flip) {
  ground.set(makeConfig(data, ply, flip));
}

function promote(ground, key, role) {
  var piece = ground.data.pieces[key];
  if (piece && piece.role === 'pawn') {
    var pieces = {};
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
  boardOrientation: boardOrientation,
  make: make,
  reload: reload,
  promote: promote,
  end: end
};
