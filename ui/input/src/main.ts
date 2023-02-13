import * as cg from 'chessground/types';
import * as xhr from 'common/xhr';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { promote } from 'chess/promotion';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { propWithEffect } from 'common';
import { VoiceCtrlImpl } from './voiceCtrl';
import { moveHandler } from './moveCtrl';
import { RootCtrl, MoveHandler,  MoveCtrl, VoiceCtrl } from './interfaces';

export { type MoveCtrl, type VoiceCtrl } from './interfaces';

export const voiceCtrl: VoiceCtrl = new VoiceCtrlImpl(); // available outside of moveCtrl

export function moveCtrl(root: RootCtrl, step: { fen: string }): MoveCtrl {
  const isFocused = propWithEffect(false, root.redraw);
  const helpModalOpen = propWithEffect(false, root.redraw);
  let handler: MoveHandler | undefined;
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
    registerHandler(h: MoveHandler) {
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
    voiceCtrl
  };
}

export function render(ctrl: MoveCtrl) {
  return h('div.input-move', [
    h('input', {
      attrs: {
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) => {
        ctrl.registerHandler(moveHandler({ input, ctrl })!);
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

function renderVoice(_: MoveCtrl) {
  return h('div#voice-move-button', '');
}

const sanToRole: { [key: string]: cg.Role } = {
  P: 'pawn',
  N: 'knight',
  B: 'bishop',
  R: 'rook',
  Q: 'queen',
  K: 'king',
};
