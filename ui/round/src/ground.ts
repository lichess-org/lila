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
import { finished, status } from 'lib/game/status';

export function makeConfig(ctrl: RoundController): CgConfig {
  const data = ctrl.data,
    hooks = ctrl.makeCgHooks(),
    step = plyStep(data, ctrl.ply),
    playing = ctrl.isPlaying(),
    gameFinished = finished(data),
    customHighlights = new Map<Key, string>();
  if (gameFinished && data.game.variant.key == 'standard') {
    const gameStatus = data.game.status.id;
    const winner = data.game.winner,
      loser = winner === 'white' ? 'black' : winner === 'black' ? 'white' : undefined;
    if (winner) {
      const winningKingSquare = util.findKingSquare(step.fen, winner);
      if (winningKingSquare) {
        customHighlights.set(winningKingSquare as Key, 'king-win');
      }
    }
    if (loser) {
      const losingKingSquare = util.findKingSquare(step.fen, loser);
      if (losingKingSquare) {
        if (gameStatus === status.mate) {
          customHighlights.set(losingKingSquare as Key, 'king-lose-checkmate');
        } else if (gameStatus === status.outoftime || gameStatus === status.timeout) {
          customHighlights.set(losingKingSquare as Key, 'king-lose-timeout');
        } else if (gameStatus === status.resign) {
          customHighlights.set(losingKingSquare as Key, 'king-lose-resign');
        }
      }
    } else if (
      gameStatus === status.draw ||
      gameStatus === status.stalemate ||
      gameStatus === status.timeout
    ) {
      const white_king = util.findKingSquare(step.fen, 'white');
      const black_king = util.findKingSquare(step.fen, 'black');
      customHighlights.set(white_king as Key, 'king-draw');
      customHighlights.set(black_king as Key, 'king-draw');
    }
  }
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
    touchIgnoreRadius: data.correspondence ? 0 : 1,
    highlight: {
      lastMove: data.pref.highlight,
      check: data.pref.highlight,
      custom: customHighlights,
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
      unrestrictedPremoves: data.game.variant.key === 'atomic',
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
