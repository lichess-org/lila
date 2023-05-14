import { Role, Square } from 'shogiops/types';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { Api as SgApi } from 'shogiground/api';
import { makeUsi, parseSquareName } from 'shogiops';

export type KeyboardMoveHandler = (
  variant: VariantKey,
  sfen: Sfen,
  lastSquare: Square | undefined,
  yourMove: boolean
) => void;

interface ClockController {
  millisOf: (color: Color) => number;
}
export interface KeyboardMove {
  move(orig: Key, dest: Key, promotion: boolean): void;
  drop(key: Key, role: Role): void;
  sg: SgApi;
  update(step: Step, yourMove?: boolean): void;
  registerHandler(h: KeyboardMoveHandler): void;
  hasFocus(): boolean;
  setFocus(v: boolean): void;
  select(key: Key): void;
  hasSelected(): Key | undefined;
  confirmMove(): void;
  usedMove: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockController | undefined;
  draw(): void;
  next(): void;
  vote(v: boolean): void;
  resign(v: boolean, immediately?: boolean): void;
}

export interface RootData {
  game: { variant: { key: VariantKey } };
  player: { color: Color };
}
export interface RootController {
  shogiground: SgApi;
  clock?: ClockController;
  data: RootData;
  sendUsi?: (usi: string, meta: {}) => void;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  resign?: (v: boolean, immediately?: boolean) => void;
  submitUsi?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: Redraw;
  next?: () => void;
  vote?: (v: boolean) => void;
}
interface Step {
  sfen: Sfen;
  lastSquare: Square | undefined;
}
type Redraw = () => void;

export function ctrl(root: RootController, step: Step): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step;
  let lastSelect = performance.now();
  const sgState = root.shogiground.state;
  const select = (key: Key, prom?: boolean): void => {
    if (sgState.selected === key) root.shogiground.cancelMove();
    else {
      root.shogiground.selectSquare(key, prom, true);
      lastSelect = performance.now();
    }
  };
  let usedMove = false;
  return {
    move(orig, dest, prom) {
      usedMove = true;
      root.shogiground.cancelMove();
      root.shogiground.move(orig, dest, prom);
      const usi = orig + dest + (prom ? '+' : '');
      root.sendUsi && root.sendUsi(usi, {});
    },
    drop(key, role) {
      const color = root.data.player.color;
      root.shogiground.cancelMove();
      root.shogiground.drop({ role, color }, key);
      const usi = makeUsi({ role: role, to: parseSquareName(key) });
      root.sendUsi && root.sendUsi(usi, {});
    },
    sg: root.shogiground,
    update(step, yourMove: boolean = false) {
      if (handler) handler(root.data.game.variant.key, step.sfen, step.lastSquare, yourMove);
      else preHandlerBuffer = step;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer)
        handler(root.data.game.variant.key, preHandlerBuffer.sfen, preHandlerBuffer.lastSquare, false);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      root.redraw();
    },
    select,
    hasSelected: () => sgState.selected,
    confirmMove: () => root.submitUsi?.(true),
    usedMove,
    jump(plyDelta: number) {
      // root.userJump(root.ply + delta);
      root.userJumpPlyDelta && root.userJumpPlyDelta(plyDelta);
      root.redraw();
    },
    justSelected: () => performance.now() - lastSelect < 500,
    clock: () => root.clock,
    draw: () => (root.offerDraw ? root.offerDraw(true, true) : null),
    resign: (v, immediately) => (root.resign ? root.resign(v, immediately) : null),
    next: () => root.next?.(),
    vote: (v: boolean) => root.vote?.(v),
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: 'false',
        autocomplete: 'off',
      },
      hook: onInsert(input =>
        window.lishogi
          .loadScript(`compiled/lishogi.keyboardMove${document.body.getAttribute('data-dev') ? '' : '.min'}.js`)
          .then(() => {
            ctrl.registerHandler(
              window.lishogi.keyboardMove({
                input,
                ctrl,
              })
            );
          })
      ),
    }),
    ctrl.hasFocus() ? h('em', 'Press <enter> submit your command') : h('strong', 'Press <enter> to focus'),
  ]);
}
