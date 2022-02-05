import { h } from 'snabbdom';
import * as cg from 'shogiground/types';
import { Step, Redraw } from './interfaces';
import RoundController from './ctrl';
import { ClockController } from './clock/clockCtrl';
import { valid as handValid } from './hands/handCtrl';
import { sendPromotion } from './promotion';
import { onInsert } from './util';
import { parseHands } from 'shogiops/sfen';
import { lastStep } from './round';

export type KeyboardMoveHandler = (sfen: Sfen, dests?: cg.Dests, yourMove?: boolean) => void;

export interface KeyboardMove {
  drop(key: cg.Key, piece: string): void;
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(step: Step, yourMove?: boolean): void;
  registerHandler(h: KeyboardMoveHandler): void;
  hasFocus(): boolean;
  setFocus(v: boolean): void;
  move(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedMove: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockController | undefined;
}

const sanToRole: { [key: string]: cg.Role } = {
  P: 'pawn',
  L: 'lance',
  N: 'knight',
  S: 'silver',
  G: 'gold',
  B: 'bishop',
  R: 'rook',
  K: 'king',
  H: 'horse',
  D: 'dragon',
};

export function ctrl(root: RoundController, step: Step, redraw: Redraw): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.sfen;
  let lastSelect = Date.now();
  const cgState = root.shogiground.state;
  const select = function (key: cg.Key): void {
    if (cgState.selected === key) root.shogiground.cancelMove();
    else {
      root.shogiground.selectSquare(key, true);
      lastSelect = Date.now();
    }
  };
  let usedMove = false;
  return {
    drop(key, piece) {
      const role = sanToRole[piece];
      const color = root.data.player.color;
      const parsedHands = parseHands(lastStep(root.data).sfen.split(' ')[2] || '-');
      // Square occupied
      if (!role || parsedHands.isErr || cgState.pieces[key]) return;
      // Piece not in hand
      if (parsedHands.value[color][role] === 0) return;
      if (!handValid(root, role, key)) return;
      root.shogiground.cancelMove();
      root.shogiground.newPiece({ role, color }, key);
      root.sendNewPiece(role, key, false);
    },
    promote(orig, dest, piece) {
      const role = sanToRole[piece];
      if (!role) return;
      root.shogiground.cancelMove();
      sendPromotion(root, orig, dest, role, { premove: false });
    },
    update(step, yourMove: boolean = false) {
      if (handler) handler(step.sfen, cgState.movable.dests, yourMove);
      else preHandlerBuffer = step.sfen;
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
    move(orig, dest) {
      usedMove = true;
      root.shogiground.cancelMove();
      select(orig);
      select(dest);
    },
    select,
    hasSelected: () => cgState.selected,
    confirmMove() {
      root.submitMove(true);
    },
    usedMove,
    jump(delta: number) {
      root.userJump(root.ply + delta);
      redraw();
    },
    justSelected() {
      return Date.now() - lastSelect < 500;
    },
    clock: () => root.clock,
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: false,
        autocomplete: false,
      },
      hook: onInsert(el => {
        window.lishogi.loadScript('compiled/lishogi.round.keyboardMove.min.js').then(() => {
          ctrl.registerHandler(
            window.lishogi.keyboardMove({
              input: el,
              ctrl,
            })
          );
        });
      }),
    }),
    ctrl.hasFocus()
      ? // todo
        h('em', 'Enter USI (2a3c) or (2133) to make a move, or type / to focus chat')
      : h('strong', 'Press <enter> to focus'),
  ]);
}
