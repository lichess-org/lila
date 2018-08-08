import { Draughtsground } from 'draughtsground';
import * as cg from 'draughtsground/types';
import { Api as CgApi } from 'draughtsground/api';
import { countGhosts } from 'draughtsground/fen';
import { Config } from 'draughtsground/config'
import * as util from './util';
import { plyStep } from './round';
import RoundController from './ctrl';
import { RoundData } from './interfaces';

import { h } from 'snabbdom'

function makeConfig(ctrl: RoundController): Config {

    const data = ctrl.data, hooks = ctrl.makeCgHooks(),
        step = plyStep(data, ctrl.ply),
        playing = ctrl.isPlaying();

    const ghosts = countGhosts(step.fen);

    return {
        fen: step.fen,
        orientation: boardOrientation(data, ctrl.flip),
        turnColor: (step.ply - (ghosts == 0 ? 0 : 1)) % 2 === 0 ? 'white' : 'black',
        lastMove: util.uci2move(step.uci),
        captureLength: data.captureLength,
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
            color: playing ? data.player.color : undefined,
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
            variant: data.game.variant.key,
            events: {
                set: hooks.onPremove,
                unset: hooks.onCancelPremove
            }
        },
        predroppable: {
            enabled: data.pref.enablePremove && data.game.variant.key === 'crazyhouse',
            events: {
                set: hooks.onPredrop,
                unset() { hooks.onPredrop(undefined) }
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

export function reload(ctrl: RoundController) {
  ctrl.draughtsground.set(makeConfig(ctrl));
}

export function promote(ground: CgApi, key: cg.Key, role: cg.Role) {
  const piece = ground.state.pieces[key];
  //if (piece && piece.role === 'pawn') {
  if (piece && piece.role === 'man') {
    const pieces: cg.Pieces = {};
    pieces[key] = {
      color: piece.color,
      role,
      promoted: true
    };
    ground.setPieces(pieces);
  }
}

export function boardOrientation(data: RoundData, flip: boolean): Color {
  if (data.game.variant.key === 'racingKings') return flip ? 'black': 'white';
  else return flip ? data.opponent.color : data.player.color;
}

export function render(ctrl: RoundController) {
  return h('div.cg-board-wrap', {
    hook: {
      insert(vnode) {
        ctrl.setDraughtsground(Draughtsground((vnode.elm as HTMLElement), makeConfig(ctrl)));
      }
    }
  }, [
    h('div.cg-board')
  ]);
};
