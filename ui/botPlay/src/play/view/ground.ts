import resizeHandle from 'common/chessgroundResize';
import type PlayCtrl from '../playCtrl';
import { ShowResizeHandle, Coords, MoveEvent } from 'common/prefs';
import { storage } from 'common/storage';
import { makeFen } from 'chessops/fen';
import { chessgroundDests, chessgroundMove } from 'chessops/compat';
import { Board } from '../chess';

export const updateGround = (board: Board): CgConfig => ({
  fen: makeFen(board.chess.toSetup()),
  check: board.chess.isCheck(),
  turnColor: board.chess.turn,
  lastMove: board.lastMove && chessgroundMove(board.lastMove),
  movable: {
    dests: board.isEnd ? new Map() : chessgroundDests(board.chess),
  },
});

export function initialGround(ctrl: PlayCtrl): CgConfig {
  const playing = ctrl.isPlaying();
  const pref = ctrl.opts.pref;
  const chess = ctrl.board.chess;
  return {
    fen: makeFen(chess.toSetup()),
    orientation: ctrl.game.pov,
    turnColor: chess.turn,
    lastMove: ctrl.board.lastMove && chessgroundMove(ctrl.board.lastMove),
    check: chess.isCheck(),
    coordinates: pref.coords !== Coords.Hidden,
    coordinatesOnSquares: pref.coords === Coords.All,
    addPieceZIndex: pref.is3d,
    addDimensionsCssVarsTo: document.body,
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    events: {
      select: ctrl.onPieceSelect,
      insert: elements =>
        resizeHandle(
          elements,
          playing ? pref.resizeHandle : ShowResizeHandle.Always,
          ctrl.board.onPly,
          p => p <= 2,
        ),
    },
    movable: {
      free: false,
      color: playing ? ctrl.game.pov : undefined,
      dests: playing ? chessgroundDests(chess) : new Map(),
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
