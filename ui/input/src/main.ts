import * as cg from 'chessground/types';
import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { promote } from 'chess/promotion';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { propWithEffect, Prop } from 'common';
import { makeKeyboardMove } from './inputMove';
import { RootCtrl, ClockCtrl, InputMoveHandler } from './interfaces';

export interface InputMoveCtrl {
  drop(key: cg.Key, piece: string): void;
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(step: { fen: string }, yourMove?: boolean): void;
  registerHandler(h: InputMoveHandler): void;
  isFocused: Prop<boolean>;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockCtrl | undefined;
  draw(): void;
  next(): void;
  vote(v: boolean): void;
  resign(v: boolean, immediately?: boolean): void;
  helpModalOpen: Prop<boolean>;
}

const sanToRole: { [key: string]: cg.Role } = {
  P: 'pawn',
  N: 'knight',
  B: 'bishop',
  R: 'rook',
  Q: 'queen',
  K: 'king',
};

export function ctrl(root: RootCtrl, step: { fen: string }): InputMoveCtrl {
  const isFocused = propWithEffect(false, root.redraw);
  const helpModalOpen = propWithEffect(false, root.redraw);
  let handler: InputMoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  let lastSelect = performance.now();
  const cgState = root.chessground.state;
  const select = (key: cg.Key): void => {
    if (cgState.selected === key) root.chessground.cancelMove();
    else {
      root.chessground.selectSquare(key, true);
      lastSelect = performance.now();
    }
  };
  let usedSan = false;
  return {
    drop(key, piece) {
      const role = sanToRole[piece];
      const crazyData = root.data.crazyhouse;
      const color = root.data.player.color;
      // Crazyhouse not set up properly
      if (!root.crazyValid || !root.sendNewPiece) return;
      // Square occupied
      if (!role || !crazyData || cgState.pieces.has(key)) return;
      // Piece not in Pocket
      if (!crazyData.pockets[color === 'white' ? 0 : 1][role]) return;
      if (!root.crazyValid(role, key)) return;
      root.chessground.cancelMove();
      root.chessground.newPiece({ role, color }, key);
      root.sendNewPiece(role, key, false);
    },
    promote(orig, dest, piece) {
      const role = sanToRole[piece];
      const variant = root.data.game.variant.key;
      if (!role || role == 'pawn' || (role == 'king' && variant !== 'antichess')) return;
      root.chessground.cancelMove();
      promote(root.chessground, dest, role);
      root.sendMove(orig, dest, role, { premove: false });
    },
    update(step, yourMove = false) {
      if (handler) handler(step.fen, cgState.movable.dests, yourMove);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: InputMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cgState.movable.dests);
    },
    san(orig, dest) {
      usedSan = true;
      root.chessground.cancelMove();
      select(orig);
      select(dest);
      // ensure chessground does not leave the destination square selected
      root.chessground.cancelMove();
    },
    select,
    hasSelected: () => cgState.selected,
    confirmMove: () => (root.submitMove ? root.submitMove(true) : null),
    usedSan,
    jump(plyDelta: number) {
      root.userJumpPlyDelta && root.userJumpPlyDelta(plyDelta);
      root.redraw();
    },
    justSelected: () => performance.now() - lastSelect < 500,
    clock: () => root.clock,
    draw: () => (root.offerDraw ? root.offerDraw(true, true) : null),
    resign: (v, immediately) => (root.resign ? root.resign(v, immediately) : null),
    next: () => root.next?.(),
    vote: (v: boolean) => root.vote?.(v),
    helpModalOpen,
    isFocused,
  };
}

export function render(ctrl: InputMoveCtrl) {
  return h('div.input-move', [
    h('input', {
      attrs: {
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) => {
        ctrl.registerHandler(makeKeyboardMove({ input, ctrl })!);
      }),
    }),
    ctrl.isFocused()
      ? h('em', 'Enter SAN (Nc3), ICCF (2133) or UCI (b1c3) moves, type ? to learn more')
      : h('strong', 'Press <enter> to focus'),
    renderVoice(ctrl),
    ctrl.helpModalOpen()
      ? snabModal({
          class: 'keyboard-move-help',
          content: [h('div.scrollable', spinner())],
          onClose: () => ctrl.helpModalOpen(false),
          onInsert: async ($wrap: Cash) => {
            const [, html] = await Promise.all([
              lichess.loadCssPath('keyboardMove.help'),
              xhr.text(xhr.url('/help/keyboard-move', {})),
            ]);
            $wrap.find('.scrollable').html(html);
          },
        })
      : null,
  ]);
}

function renderVoice(_: InputMoveCtrl) {
  return h('div#voice-move-button', '');
}
