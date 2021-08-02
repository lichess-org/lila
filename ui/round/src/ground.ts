import { h } from 'snabbdom';
import { Chessground } from 'chessground';
import { Config } from 'chessground/config';
import changeColorHandle from 'common/coordsColor';
import * as round from './round';
import resizeHandle from 'common/resize';
import * as util from './util';
import { plyStep } from './round';
import RoundController from './ctrl';
import { RoundData } from './interfaces';

export function makeConfig(ctrl: RoundController): Config {
  const data = ctrl.data,
    hooks = ctrl.makeCgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying();
  return {
    fen: step.fen,
    orientation: boardOrientation(data, ctrl.flip),
    turnColor: step.ply % 2 === 0 ? 'white' : 'black',
    lastMove: util.uci2move(step.uci),
    check: !!step.check,
    coordinates: data.pref.coords !== Prefs.Coords.Hidden,
    addPieceZIndex: ctrl.data.pref.is3d,
    addDimensionsCssVars: true,
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
        resizeHandle(elements, ctrl.data.pref.resizeHandle, ctrl.ply, p => p <= showUntil);
        if (data.pref.coords === Prefs.Coords.Inside) changeColorHandle();
      },
    },
    movable: {
      free: false,
      color: playing ? data.player.color : undefined,
      dests: playing ? util.parsePossibleMoves(data.possibleMoves) : new Map(),
      showDests: data.pref.destination,
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
      showDests: data.pref.destination,
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
      defaultSnapToValidMove: (lichess.storage.get('arrow.snap') || 1) != '0',
    },
    disableContextMenu: true,
  };
}

export function reload(ctrl: RoundController) {
  ctrl.chessground.set(makeConfig(ctrl));
}

export function boardOrientation(data: RoundData, flip: boolean): Color {
  if (data.game.variant.key === 'racingKings') return flip ? 'black' : 'white';
  else return flip ? data.opponent.color : data.player.color;
}

export function render(ctrl: RoundController) {
  return h('div.cg-wrap', {
    hook: util.onInsert(el => ctrl.setChessground(Chessground(el, makeConfig(ctrl)))),
  });
}
