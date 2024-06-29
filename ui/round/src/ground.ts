import * as round from './round';
import * as util from './util';
import resizeHandle from 'common/resize';
import RoundController from './ctrl';
import { Config } from 'chessground/config';
import { h } from 'snabbdom';
import { plyStep } from './round';
import { RoundData } from './interfaces';
import { uciToMove } from 'chessground/util';
import * as Prefs from 'common/prefs';

export function makeConfig(ctrl: RoundController): Config {
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
    coordinates: data.pref.coords !== Prefs.Coords.Hidden,
    coordinatesOnSquares: data.pref.coords === Prefs.Coords.All,
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
        const firstPly = round.firstPly(ctrl.data);
        const isSecond = (firstPly % 2 === 0 ? 'white' : 'black') !== data.player.color;
        const showUntil = firstPly + 2 + +isSecond;
        resizeHandle(
          elements,
          playing ? ctrl.data.pref.resizeHandle : Prefs.ShowResizeHandle.Always,
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
      enabled: data.pref.moveEvent !== Prefs.MoveEvent.Click,
      showGhost: data.pref.highlight,
    },
    selectable: {
      enabled: data.pref.moveEvent !== Prefs.MoveEvent.Drag,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: site.storage.boolean('arrow.snap').getOrDefault(true),
    },
    disableContextMenu: true,
  };
}

export const reload = (ctrl: RoundController) => ctrl.chessground.set(makeConfig(ctrl));

export const boardOrientation = (data: RoundData, flip: boolean): Color =>
  data.game.variant.key === 'racingKings'
    ? flip
      ? 'black'
      : 'white'
    : flip
    ? data.opponent.color
    : data.player.color;

export const render = (ctrl: RoundController) =>
  h('div.cg-wrap', {
    hook: util.onInsert(el => ctrl.setChessground(site.makeChessground(el, makeConfig(ctrl)))),
  });
