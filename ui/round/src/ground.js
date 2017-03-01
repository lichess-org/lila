var Chessground = require('chessground').Chessground;
var isPlayerPlaying = require('game').game.isPlayerPlaying;
var util = require('./util');
var round = require('./round');
var m = require('mithril');

function makeConfig(ctrl) {
  var data = ctrl.data, hooks = ctrl.makeCgHooks();
  var step = round.plyStep(data, ctrl.vm.ply);
  var playing = isPlayerPlaying(data);
  return {
    fen: step.fen,
    orientation: boardOrientation(data, ctrl.vm.flip),
    turnColor: step.ply % 2 === 0 ? 'white' : 'black',
    lastMove: util.uci2move(step.uci),
    check: !!step.check,
    coordinates: data.pref.coords !== 0,
    autoCastle: data.game.variant.key === 'standard',
    viewOnly: data.player.spectator,
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight
    },
    events: {
      move: hooks.onMove,
      dropNewPiece: hooks.onNewPiece
    },
    movable: {
      free: false,
      color: playing ? data.player.color : null,
      dests: playing ? util.parsePossibleMoves(data.possibleMoves) : {},
      showDests: data.pref.destination,
      rookCastle: data.pref.rookCastle,
      events: {
        after: hooks.onUserMove,
        afterNewPiece: hooks.onUserNewPiece
      }
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
        set: hooks.onPremove,
        unset: hooks.onCancelPremove
      }
    },
    predroppable: {
      enabled: data.pref.enablePremove && data.game.variant.key === 'crazyhouse',
      events: {
        set: hooks.onPredrop,
        unset: hooks.onPredrop
      }
    },
    draggable: {
      enabled: data.pref.moveEvent > 0,
      showGhost: data.pref.highlight
    },
    selectable: {
      enabled: data.pref.moveEvent !== 1
    },
    drawable: {
      enabled: true
    },
    disableContextMenu: true
  };
}

function reload(ctrl) {
  ctrl.chessground.set(makeConfig(ctrl));
}

function promote(cg, key, role) {
  var piece = cg.state.pieces[key];
  if (piece && piece.role === 'pawn') {
    var pieces = {};
    pieces[key] = {
      color: piece.color,
      role: role
    };
    cg.setPieces(pieces);
  }
}

function boardOrientation(data, flip) {
  if (data.game.variant.key === 'racingKings') {
    return flip ? 'black': 'white';
  } else {
    return flip ? data.opponent.color : data.player.color;
  }
}

module.exports = {
  render: function(ctrl) {
    return m('div.cg-board-wrap', {
      config: function(el, isUpdate) {
        if (!isUpdate) ctrl.chessground = Chessground(el, makeConfig(ctrl));
      }
    });
  },
  boardOrientation: boardOrientation,
  reload: reload,
  promote: promote
};
