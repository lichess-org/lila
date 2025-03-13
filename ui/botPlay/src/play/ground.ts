import resizeHandle from 'common/chessgroundResize';
import type PlayCtrl from './playCtrl';
import { uciToMove } from 'chessground/util';
import { ShowResizeHandle, Coords, MoveEvent } from 'common/prefs';
import { storage } from 'common/storage';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export function chessgroundConfig(ctrl: PlayCtrl): CgConfig {
  const playing = ctrl.isPlaying();
  return {
    fen: makeFen(ctrl.chess.toSetup()),
    orientation: ctrl.pov,
    turnColor: ctrl.chess.turn,
    lastMove: uciToMove(ctrl.moves[ctrl.moves.length - 1]),
    check: ctrl.chess.isCheck(),
    coordinates: ctrl.pref.coords !== Coords.Hidden,
    coordinatesOnSquares: ctrl.pref.coords === Coords.All,
    addPieceZIndex: ctrl.pref.is3d,
    addDimensionsCssVarsTo: document.body,
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight,
    },
    events: {
      move: ctrl.onMove,
      insert: elements =>
        resizeHandle(
          elements,
          playing ? ctrl.pref.resizeHandle : ShowResizeHandle.Always,
          ctrl.onPly,
          p => p <= 2,
        ),
    },
    movable: {
      free: false,
      color: playing ? ctrl.pov : undefined,
      dests: playing ? chessgroundDests(ctrl.chess) : new Map(),
      showDests: ctrl.pref.destination,
      rookCastle: ctrl.pref.rookCastle,
      events: {
        after: ctrl.onUserMove,
      },
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animationDuration,
    },
    premovable: {
      enabled: ctrl.pref.enablePremove,
      showDests: ctrl.pref.destination,
      // events: {
      //   set: hooks.onPremove,
      //   unset: hooks.onCancelPremove,
      // },
    },
    draggable: {
      enabled: ctrl.pref.moveEvent !== MoveEvent.Click,
      showGhost: ctrl.pref.highlight,
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== MoveEvent.Drag,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
    },
    disableContextMenu: true,
  };
}
