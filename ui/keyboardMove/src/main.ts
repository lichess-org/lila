import * as cg from 'chessground/types';
import { Api as CgApi } from 'chessground/api';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { promote } from 'chess/promotion';
import { snabDialog } from 'common/dialog';
import { propWithEffect, Prop } from 'common';
import { Player } from 'game';
import { load as loadKeyboardMove } from './plugins/keyboardMove';
import KeyboardChecker from './plugins/keyboardChecker';

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests, yourMove?: boolean) => void;

interface ClockController {
  millisOf: (color: Color) => number;
}
export interface KeyboardMove {
  drop(key: cg.Key, piece: string): void;
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(step: Step, yourMove?: boolean): void;
  registerHandler(h: KeyboardMoveHandler): void;
  isFocused: Prop<boolean>;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockController | undefined;
  draw(): void;
  next(): void;
  vote(v: boolean): void;
  resign(v: boolean, immediately?: boolean): void;
  helpModalOpen: Prop<boolean>;
  checker?: KeyboardChecker;
  opponent?: string;
}

const sanToRole: { [key: string]: cg.Role } = {
  P: 'pawn',
  N: 'knight',
  B: 'bishop',
  R: 'rook',
  Q: 'queen',
  K: 'king',
};

interface CrazyPocket {
  [role: string]: number;
}
export interface RootData {
  crazyhouse?: { pockets: [CrazyPocket, CrazyPocket] };
  game: { variant: { key: VariantKey } };
  player: { color: Color };
  opponent?: Player;
}
export interface RootController {
  chessground: CgApi;
  clock?: ClockController;
  crazyValid?: (role: cg.Role, key: cg.Key) => boolean;
  data: RootData;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  auxMove: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined) => void;
  sendNewPiece?: (role: cg.Role, key: cg.Key, isPredrop: boolean) => void;
  submitMove?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: Redraw;
  next?: () => void;
  vote?: (v: boolean) => void;
}
interface Step {
  fen: string;
}
type Redraw = () => void;

export function ctrl(root: RootController, step: Step): KeyboardMove {
  const isFocused = propWithEffect(false, root.redraw);
  const helpModalOpen = propWithEffect(false, root.redraw);
  let handler: KeyboardMoveHandler | undefined;
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
      root.auxMove(orig, dest, role);
    },
    update(step, yourMove = false) {
      if (handler) handler(step.fen, cgState.movable.dests, yourMove);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: KeyboardMoveHandler) {
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
    checker: root.clock ? new KeyboardChecker() : undefined,
    opponent: root.data.opponent?.user?.username,
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert((input: HTMLInputElement) =>
        loadKeyboardMove({ input, ctrl }).then((m: KeyboardMoveHandler) => ctrl.registerHandler(m)),
      ),
    }),
    ctrl.isFocused()
      ? h('em', 'Enter SAN (Nc3), ICCF (2133) or UCI (b1c3) moves, type ? to learn more')
      : h('strong', 'Press <enter> to focus'),
    ctrl.helpModalOpen()
      ? snabDialog({
          class: 'help.keyboard-move-help',
          htmlUrl: '/help/keyboard-move',
          onClose: () => ctrl.helpModalOpen(false),
        })
      : null,
  ]);
}
