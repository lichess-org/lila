import * as util from './util';
import { onInsert } from 'lib/snabbdom';
import resizeHandle from 'lib/chessgroundResize';
import type RoundController from './ctrl';
import { h, type VNode } from 'snabbdom';
import { plyStep } from './util';
import type { RoundData } from './interfaces';
import { uciToMove } from '@lichess-org/chessground/util';
import { ShowResizeHandle, Coords, MoveEvent } from 'lib/prefs';
import { storage } from 'lib/storage';
import { Chessground as makeChessground } from '@lichess-org/chessground';

export function makeConfig(ctrl: RoundController): CgConfig {
  const data = ctrl.data,
    hooks = ctrl.makeCgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying();
  return {
    fen: step.fen,
    orientation: boardOrientation(data, ctrl.flip),
    turnColor: step.ply % 2 === 0 ? 'white' : 'black',
    lastMove: uciToMove(step.uci),
    check: !!step.check,
    coordinates: data.pref.coords !== Coords.Hidden,
    coordinatesOnSquares: data.pref.coords === Coords.All,
    addPieceZIndex: ctrl.data.pref.is3d,
    addDimensionsCssVarsTo: document.body,
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
    },
    events: {
      move: hooks.onMove,
      dropNewPiece: hooks.onNewPiece,
      insert(elements) {
        const firstPly = util.firstPly(ctrl.data);
        const isSecond = (firstPly % 2 === 0 ? 'white' : 'black') !== data.player.color;
        const showUntil = firstPly + 2 + +isSecond;
        resizeHandle(
          elements,
          playing ? ctrl.data.pref.resizeHandle : ShowResizeHandle.Always,
          ctrl.ply,
          p => p <= showUntil,
        );
      },
    },
    movable: {
      free: false,
      color: playing ? data.player.color : undefined,
      dests: playing ? util.parsePossibleMoves(data.possibleMoves) : new Map(),
      showDests: data.pref.destination && !ctrl.blindfold(),
      rookCastle: data.pref.rookCastle,
      events: {
        after: hooks.onUserMove,
        afterNewPiece: hooks.onUserNewPiece,
      },
    },
    animation: {
      enabled: true,
      duration: data.pref.animationDuration,
    },
    premovable: {
      enabled: data.pref.enablePremove,
      showDests: data.pref.destination && !ctrl.blindfold(),
      castle: data.game.variant.key !== 'antichess',
      events: {
        set: hooks.onPremove,
        unset: hooks.onCancelPremove,
      },
      premoveThroughFriendlies: data.game.variant.key === 'atomic',
    },
    predroppable: {
      enabled: data.pref.enablePremove && data.game.variant.key === 'crazyhouse',
      events: {
        set: hooks.onPredrop,
        unset() {
          hooks.onPredrop(undefined);
        },
      },
    },
    draggable: {
      enabled: data.pref.moveEvent !== MoveEvent.Click,
      showGhost: data.pref.highlight,
    },
    selectable: {
      enabled: data.pref.moveEvent !== MoveEvent.Drag,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
    },
    disableContextMenu: true,
  };
}

export const reload = (ctrl: RoundController): void => ctrl.chessground.set(makeConfig(ctrl));

export const boardOrientation = (data: RoundData, flip: boolean): Color =>
  data.game.variant.key === 'racingKings'
    ? flip
      ? 'black'
      : 'white'
    : flip
      ? data.opponent.color
      : data.player.color;

export const render = (ctrl: RoundController): VNode =>
  h('div.cg-wrap', {
    hook: onInsert(el => ctrl.setChessground(makeChessground(el, makeConfig(ctrl)))),
  });
