import * as sg from 'shogiground/types';
import { Role } from 'shogiops/types';
import { h } from 'snabbdom';
import { ClockController } from './clock/clockCtrl';
import RoundController from './ctrl';
import { Redraw, Step } from './interfaces';
import { onInsert } from './util';

export type KeyboardMoveHandler = (
  sfen: Sfen,
  moveDests: sg.MoveDests | undefined,
  dropDests: sg.DropDests | undefined,
  yourMove?: boolean
) => void;

export interface KeyboardMove {
  move(orig: Key, dest: Key, promotion: boolean): void;
  drop(key: Key, role: Role): void;
  lastDest: Key | undefined;
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
}

export function ctrl(root: RoundController, step: Step, redraw: Redraw): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.sfen;
  let lastSelect = Date.now();
  const sgState = root.shogiground.state;
  const select = function (key: Key, prom?: boolean): void {
    if (sgState.selected === key) root.shogiground.cancelMove();
    else {
      root.shogiground.selectSquare(key, prom, true);
      lastSelect = Date.now();
    }
  };
  let usedMove = false;
  return {
    move(orig, dest, prom) {
      usedMove = true;
      root.shogiground.cancelMove();
      root.shogiground.move(orig, dest, prom);
    },
    drop(key, role) {
      const color = root.data.player.color;
      // Piece not in hand
      root.shogiground.cancelMove();
      root.shogiground.drop({ role, color }, key);
    },
    lastDest: sgState.lastDests && sgState.lastDests[sgState.lastDests.length - 1],
    update(step, yourMove: boolean = false) {
      if (handler) handler(step.sfen, sgState.movable.dests, sgState.droppable.dests, yourMove);
      else preHandlerBuffer = step.sfen;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, sgState.movable.dests, sgState.droppable.dests);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      redraw();
    },
    select,
    hasSelected: () => sgState.selected,
    confirmMove() {
      root.submitUsi(true);
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
