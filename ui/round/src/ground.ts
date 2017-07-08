import { Chessground } from 'chessground';
import { Config } from 'chessground/config'
import { game } from 'game';
import * as util from './util';
import * as round from './round';

import { h } from 'snabbdom'

function makeConfig(ctrl): Config {
  var data = ctrl.data, hooks = ctrl.makeCgHooks();
  var step = round.plyStep(data, ctrl.vm.ply);
  var playing = game.isPlayerPlaying(data);
  return {
    fen: step.fen,
    orientation: boardOrientation(data, ctrl.vm.flip),
    turnColor: step.ply % 2 === 0 ? 'white' : 'black',
    lastMove: util.uci2move(step.uci),
    check: !!step.check,
    coordinates: data.pref.coords !== 0,
    addPieceZIndex: ctrl.data.pref.is3d,
    autoCastle: data.game.variant.key === 'standard',
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

export function reload(ctrl) {
  ctrl.chessground.set(makeConfig(ctrl));
}

export function promote(cg, key, role) {
  var piece = cg.state.pieces[key];
  if (piece && piece.role === 'pawn') {
    var pieces = {};
    pieces[key] = {
      color: piece.color,
      role,
      promoted: true
    };
    cg.setPieces(pieces);
  }
}

export function boardOrientation(data, flip) {
  if (data.game.variant.key === 'racingKings') return flip ? 'black': 'white';
  else return flip ? data.opponent.color : data.player.color;
}

export function render(ctrl) {
  return h('div.cg-board-wrap', {
    hook: {
      insert: vnode => {
        ctrl.setChessground(Chessground((vnode.elm as HTMLElement), makeConfig(ctrl)));
      }
    }
  }, [
    h('div.cg-board')
  ]);
};
