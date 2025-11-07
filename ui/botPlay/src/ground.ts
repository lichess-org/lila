import resizeHandle from 'lib/chessgroundResize';
import type PlayCtrl from './play/playCtrl';
import { ShowResizeHandle, Coords, MoveEvent } from 'lib/prefs';
import { storage } from 'lib/storage';
import { makeFen } from 'chessops/fen';
import { chessgroundDests, chessgroundMove } from 'chessops/compat';
import type { Board } from './chess';
import { h } from 'snabbdom';
import { initMiniBoard } from 'lib/view/miniBoard';
import { makeUci } from 'chessops';
import type { Game } from './game';

export const updateGround = (game: Game, board: Board): CgConfig => {
  const onLastPosition = board.onPly === game.ply();
  const onEndPosition = onLastPosition && game.end;
  return {
    fen: fenOf(board),
    check: board.chess.isCheck(),
    turnColor: board.chess.turn,
    lastMove: board.lastMove && chessgroundMove(board.lastMove),
    movable: {
      dests: onEndPosition || board.chess.isEnd() ? new Map() : chessgroundDests(board.chess),
    },
  };
};

const lastMove = (board: Board) => board.lastMove && chessgroundMove(board.lastMove);
const fenOf = (board: Board) => makeFen(board.chess.toSetup());

export function initialGround(ctrl: PlayCtrl): CgConfig {
  const playing = ctrl.isPlaying();
  const pref = ctrl.opts.pref;
  const chess = ctrl.board.chess;
  return {
    fen: makeFen(chess.toSetup()),
    orientation: ctrl.bottomColor(),
    turnColor: chess.turn,
    lastMove: lastMove(ctrl.board),
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
      showDests: pref.destination && !ctrl.blindfold(),
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

export const miniBoard = (board: Board, pov: Color) =>
  h('span.mini-board.is2d', {
    attrs: {
      'data-state': `${fenOf(board)},${pov},${board.lastMove ? makeUci(board.lastMove) : ''}`,
    },
    hook: { insert: vnode => initMiniBoard(vnode.elm as HTMLElement) },
  });
