import resizeHandle from 'common/chessgroundResize';
import type PlayCtrl from './playCtrl';
import { uciToMove } from 'chessground/util';
import { ShowResizeHandle, Coords, MoveEvent } from 'common/prefs';
import { storage } from 'common/storage';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export function chessgroundConfig(ctrl: PlayCtrl): CgConfig {
  const playing = ctrl.isPlaying();
  const pref = ctrl.opts.pref;
  return {
    fen: makeFen(ctrl.chess.toSetup()),
    orientation: ctrl.game.pov,
    turnColor: ctrl.chess.turn,
    lastMove: uciToMove(ctrl.game.sans[ctrl.game.sans.length - 1]),
    check: ctrl.chess.isCheck(),
    coordinates: pref.coords !== Coords.Hidden,
    coordinatesOnSquares: pref.coords === Coords.All,
    addPieceZIndex: pref.is3d,
    addDimensionsCssVarsTo: document.body,
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    events: {
      move: ctrl.onMove,
      insert: elements =>
        resizeHandle(
          elements,
          playing ? pref.resizeHandle : ShowResizeHandle.Always,
          ctrl.onPly,
          p => p <= 2,
        ),
    },
    movable: {
      free: false,
      color: playing ? ctrl.game.pov : undefined,
      dests: playing ? chessgroundDests(ctrl.chess) : new Map(),
      showDests: pref.destination,
      rookCastle: pref.rookCastle,
      events: {
        after: ctrl.onUserMove,
      },
    },
    animation: {
      enabled: true,
      duration: pref.animationDuration,
    },
    premovable: {
      enabled: pref.enablePremove,
      showDests: pref.destination,
      // events: {
      //   set: hooks.onPremove,
      //   unset: hooks.onCancelPremove,
      // },
    },
    draggable: {
      enabled: pref.moveEvent !== MoveEvent.Click,
      showGhost: pref.highlight,
    },
    selectable: {
      enabled: pref.moveEvent !== MoveEvent.Drag,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
    },
    disableContextMenu: true,
  };
}
