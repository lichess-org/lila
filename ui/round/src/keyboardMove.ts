import { h } from 'snabbdom'
import * as cg from 'draughtsground/types';
import { Step, Redraw } from './interfaces';
import RoundController from './ctrl';
import { onInsert } from './util'

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests, captLen?: number) => void;

export interface KeyboardMove {
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
  let focus = true;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  const dgState = root.draughtsground.state;
  const select = function(key: cg.Key): void {
    if (dgState.selected === key) root.draughtsground.cancelMove();
    else root.draughtsground.selectSquare(key, true);
  };
  let usedSan = false;
  return {
    update(step) {
      if (handler) handler(step.fen, dgState.movable.dests, dgState.movable.captLen);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, dgState.movable.dests, dgState.movable.captLen);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      redraw();
    },
    san(orig, dest) {
      usedSan = true;
      root.draughtsground.cancelMove();
      select(orig);
      select(dest);
    },
    select,
    hasSelected: () => dgState.selected,
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
        autocomplete: false,
        autofocus: true
      },
      hook: onInsert(el => {
        window.lidraughts.loadScript('compiled/lidraughts.round.keyboardMove.min.js').then(() => {
          ctrl.registerHandler(window.lidraughts.keyboardMove({
            input: el,
            setFocus: ctrl.setFocus,
            select: ctrl.select,
            hasSelected: ctrl.hasSelected,
            confirmMove: ctrl.confirmMove,
            san: ctrl.san
          }));
        });
      })
    }),
    ctrl.hasFocus() ?
    h('em', 'Enter moves (14x3, 5-10) or squares (1403, 0510), or type / to focus chat') :
    h('strong', 'Press <enter> to focus')
  ]);
}
