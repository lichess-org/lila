import * as cg from 'chessground/types';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { promote } from 'chess/promotion';
import { propWithEffect, Prop } from 'common';
import { MoveRootCtrl, MoveUpdate } from 'chess/moveRootCtrl';
import { load as loadKeyboardMove } from './plugins/keyboardMove';
import KeyboardChecker from './plugins/keyboardChecker';

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests, yourMove?: boolean) => void;

export interface KeyboardMove {
  drop(key: cg.Key, piece: string): void;
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(up: MoveUpdate): void;
  registerHandler(h: KeyboardMoveHandler): void;
  isFocused: Prop<boolean>;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  draw(): void;
  next(): void;
  vote(v: boolean): void;
  resign(v: boolean, immediately?: boolean): void;
  helpModalOpen: Prop<boolean>;
  checker?: KeyboardChecker;
  opponent?: string;
  speakClock?: () => void;
  goBerserk?: () => void;
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
  opponent?: { color: Color; user?: { username: string } };
}

export interface KeyboardMoveRootCtrl extends MoveRootCtrl {
  sendNewPiece?: (role: cg.Role, key: cg.Key, isPredrop: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  sendMove?: (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta: cg.MoveMetadata) => void;
  submitMove?: (v: boolean) => void;
  crazyValid?: (role: cg.Role, key: cg.Key) => boolean;
  data: RootData;
}

export function ctrl(root: KeyboardMoveRootCtrl): KeyboardMove {
  const isFocused = propWithEffect(false, root.redraw);
  const helpModalOpen = propWithEffect(false, root.redraw);
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer: string | undefined;
  let lastSelect = performance.now();
  let cg: CgApi;
  const select = (key: cg.Key): void => {
    if (cg.state.selected === key) cg.cancelMove();
    else {
      cg.selectSquare(key, true);
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
      if (!role || !crazyData || cg.state.pieces.has(key)) return;
      // Piece not in Pocket
      if (!crazyData.pockets[color === 'white' ? 0 : 1][role]) return;
      if (!root.crazyValid(role, key)) return;
      cg.cancelMove();
      cg.newPiece({ role, color }, key);
      root.sendNewPiece(role, key, false);
    },
    promote(orig, dest, piece) {
      const role = sanToRole[piece];
      const variant = root.data.game.variant.key;
      if (!role || role == 'pawn' || (role == 'king' && variant !== 'antichess')) return;
      cg.cancelMove();
      promote(cg, dest, role);
      root.auxMove(orig, dest, role);
    },
    update(up: MoveUpdate) {
      if (up.cg) cg = up.cg;
      if (handler) handler(up.fen, cg.state.movable.dests, up.canMove);
      else preHandlerBuffer = up.fen;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cg.state.movable.dests);
    },
    san(orig, dest) {
      usedSan = true;
      cg.cancelMove();
      select(orig);
      select(dest);
      // ensure chessground does not leave the destination square selected
      cg.cancelMove();
    },
    select,
    hasSelected: () => cg.state.selected,
    confirmMove: () => (root.submitMove ? root.submitMove(true) : null),
    usedSan,
    jump(plyDelta: number) {
      root.userJumpPlyDelta && root.userJumpPlyDelta(plyDelta);
      root.redraw();
    },
    justSelected: () => performance.now() - lastSelect < 500,
    draw: () => (root.offerDraw ? root.offerDraw(true, true) : null),
    resign: (v, immediately) => (root.resign ? root.resign(v, immediately) : null),
    next: () => root.nextPuzzle?.(),
    vote: (v: boolean) => root.vote?.(v),
    helpModalOpen,
    isFocused,
    checker: root.speakClock ? new KeyboardChecker() : undefined,
    opponent: root.data.opponent?.user?.username,
    speakClock: root.speakClock,
    goBerserk: root.goBerserk,
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: { spellcheck: 'false', autocomplete: 'off' },
      hook: onInsert((input: HTMLInputElement) =>
        loadKeyboardMove({ input, ctrl }).then((m: KeyboardMoveHandler) => ctrl.registerHandler(m)),
      ),
    }),
    ctrl.isFocused()
      ? h('em', 'Enter SAN (Nc3), ICCF (2133) or UCI (b1c3) moves, type ? to learn more')
      : h('strong', 'Press <enter> to focus'),
    ctrl.helpModalOpen()
      ? lichess.dialog.snab({
          class: 'help.keyboard-move-help',
          htmlUrl: '/help/keyboard-move',
          onClose: () => ctrl.helpModalOpen(false),
        })
      : null,
  ]);
}
