import { h } from 'snabbdom'
import { sanToRole } from 'chess'
import * as cg from 'chessground/types';
import { Step, Redraw } from './interfaces';
import RoundController from './ctrl';
import { valid as crazyValid } from './crazy/crazyCtrl';
import { sendPromotion } from './promotion'

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests) => void;

interface SanMap {
  [key: string]: cg.Role;
}

export interface KeyboardMove {
  drop(key: cg.Key, piece: string): void
  promote(dest: cg.Key, piece: string): void
  update(step: Step): void;
  registerHandler(h: KeyboardMoveHandler): void
  hasFocus(): boolean;
  setFocus(v: boolean): void;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
}

export function ctrl(root: RoundController, step: Step, redraw: Redraw): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  const cgState = root.chessground.state;
  const sanMap = sanToRole as SanMap;
  const select = function(key: cg.Key): void {
    if (cgState.selected === key) root.chessground.cancelMove();
    else root.chessground.selectSquare(key, true);
  };
  let usedSan = false;
  return {
    drop(key, piece) {
      const role = sanMap[piece];
      const crazyData = root.data.crazyhouse;
      const color = root.data.player.color;
      // Square occupied
      if (!role || !crazyData || cgState.pieces[key]) return;
      // Piece not in Pocket
      if (!crazyData.pockets[color === 'white' ? 0 : 1][role]) return;
      if (!crazyValid(root.data, role, key)) return;
      root.chessground.cancelMove();
      root.chessground.newPiece({ role, color }, key);
      root.sendNewPiece(role, key, false);
    },
    promote(dest, piece) {
      const role = sanMap[piece];
      if (!role || role == 'pawn') return;
      root.chessground.cancelMove();
      const orig = dest.replace(/1/, '2').replace(/8/,'7');
      sendPromotion(root, orig as cg.Key, dest, role, {premove: false});
    },
    update(step) {
      if (handler) handler(step.fen, cgState.movable.dests);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cgState.movable.dests);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      redraw();
    },
    san(orig, dest) {
      usedSan = true;
      root.chessground.cancelMove();
      select(orig);
      select(dest);
    },
    select,
    hasSelected: () => cgState.selected,
    confirmMove() {
      root.submitMove(true);
    },
    usedSan
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: false,
        autocomplete: false
      },
      hook: {
        insert: vnode => {
          window.lichess.loadScript('compiled/lichess.round.keyboardMove.min.js').then(() => {
            ctrl.registerHandler(window.lichess.keyboardMove({
              input: vnode.elm,
              setFocus: ctrl.setFocus,
              select: ctrl.select,
              hasSelected: ctrl.hasSelected,
              confirmMove: ctrl.confirmMove,
              san: ctrl.san,
              drop: ctrl.drop,
              promote: ctrl.promote
            }));
          });
        }
      }
    }),
    ctrl.hasFocus() ?
    h('em', 'Enter SAN (Nc3) or UCI (b1c3) moves, or type / to focus chat') :
    h('strong', 'Press <enter> to focus')
  ]);
}
