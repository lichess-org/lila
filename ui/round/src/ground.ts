import { h } from 'snabbdom'
import { Draughtsground } from 'draughtsground';
import * as cg from 'draughtsground/types';
import { Api as CgApi } from 'draughtsground/api';
import { countGhosts } from 'draughtsground/fen';
import { Config } from 'draughtsground/config'
import resizeHandle from 'common/resize';
import * as util from './util';
import { plyStep } from './round';
import RoundController from './ctrl';
import { RoundData } from './interfaces';

export function makeConfig(ctrl: RoundController): Config {
  const data = ctrl.data, hooks = ctrl.makeCgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying(),
    ghosts = countGhosts(step.fen),
    noAssistance = data.simul && data.simul.noAssistance;
  return {
    fen: step.fen,
    orientation: boardOrientation(data, ctrl.flip),
    boardSize: data.game.variant.board.size,
    turnColor: (step.ply - (ghosts == 0 ? 0 : 1)) % 2 === 0 ? 'white' : 'black',
    lastMove: util.uci2move(step.uci),
    captureLength: data.captureLength,
    coordinates: data.pref.coords,
    coordSystem: ctrl.coordSystem(),
    addPieceZIndex: ctrl.data.pref.is3d,
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      kingMoves: !noAssistance && data.pref.showKingMoves && (data.game.variant.key === 'frisian' || data.game.variant.key === 'frysk')
    },
    events: {
      move: hooks.onMove,
      dropNewPiece: hooks.onNewPiece,
      insert(elements) {
        resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.ply);
      }
    },
    movable: {
      free: false,
      color: playing ? data.player.color : undefined,
      dests: playing ? util.parsePossibleMoves(data.possibleMoves) : {},
      showDests: !noAssistance && data.pref.destination,
      variant: data.game.variant.key,
      events: {
        after: hooks.onUserMove
      }
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration
    },
    premovable: {
      enabled: !noAssistance && data.pref.enablePremove,
      showDests: !noAssistance && data.pref.destination,
      castle: false,
      variant: data.game.variant.key
    },
    predroppable: {
      enabled: false
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
  return flip ? data.opponent.color : data.player.color;
}

export function render(ctrl: RoundController) {
  return h('div.cg-wrap', {
    hook: util.onInsert(el => ctrl.setDraughtsground(Draughtsground(el, makeConfig(ctrl))))
  });
};
