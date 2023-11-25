import { Role, Square } from 'shogiops/types';
import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import { Api as SgApi } from 'shogiground/api';
import { userMove, userDrop } from 'shogiground/board';
import { anim } from 'shogiground/anim';

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
  update(step: Step): void;
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
  resign(v: boolean): void;
  trans: Trans;
}

export interface RootData {
  game: { variant: { key: VariantKey } };
  player: { color: Color };
}
export interface RootController {
  shogiground: SgApi;
  clock?: ClockController;
  data: RootData;
  offerDraw?: (v: boolean, immediately?: boolean) => void;
  resign?: (v: boolean, force?: boolean) => void;
  submitUsi?: (v: boolean) => void;
  userJumpPlyDelta?: (plyDelta: Ply) => void;
  redraw: Redraw;
  next?: () => void;
  vote?: (v: boolean) => void;
  trans: Trans;
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
    if (sgState.selected === key) root.shogiground.cancelMoveOrDrop();
    else {
      root.shogiground.selectSquare(key, prom, true);
      lastSelect = performance.now();
    }
  };
  let usedMove = false;
  return {
    move(orig, dest, prom) {
      usedMove = true;
      root.shogiground.cancelMoveOrDrop();
      anim(state => userMove(state, orig, dest, prom), root.shogiground.state);
    },
    drop(key, role) {
      const color = root.data.player.color;
      root.shogiground.cancelMoveOrDrop();
      anim(state => userDrop(state, { role, color }, key), root.shogiground.state);
    },
    sg: root.shogiground,
    update(step) {
      if (handler)
        handler(
          root.data.game.variant.key,
          step.sfen,
          step.lastSquare,
          root.shogiground.state.activeColor === root.shogiground.state.turnColor
        );
      else preHandlerBuffer = step;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer)
        handler(
          root.data.game.variant.key,
          preHandlerBuffer.sfen,
          preHandlerBuffer.lastSquare,
          root.shogiground.state.activeColor === root.shogiground.state.turnColor
        );
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
      root.userJumpPlyDelta && root.userJumpPlyDelta(plyDelta);
      root.redraw();
    },
    justSelected: () => performance.now() - lastSelect < 500,
    clock: () => root.clock,
    draw: () => (root.offerDraw ? root.offerDraw(true, true) : null),
    resign: v => (root.resign ? root.resign(v, true) : null),
    next: () => root.next?.(),
    vote: (v: boolean) => root.vote?.(v),
    trans: root.trans,
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
    ctrl.hasFocus()
      ? h('em', ctrl.trans('pressXtoSubmit', '<enter>'))
      : h('strong', ctrl.trans('pressXtoFocus', '<enter>')),
  ]);
}
