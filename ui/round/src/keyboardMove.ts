import { h } from 'snabbdom'
import { sanToRole } from 'chess'
import * as cg from 'chessground/types';
import { Step, Redraw } from './interfaces';
import RoundController from './ctrl';
import { valid as crazyValid } from './crazy/crazyCtrl';

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests, possibleDrops?: string) => void;

interface sanMap {
  [key: string]: cg.Role;
}

export interface KeyboardMove {
  drop(orig: cg.Key, piece: string): void
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
  const select = function(key: cg.Key): void {
    if (root.chessground.state.selected === key) root.chessground.cancelMove();
    else root.chessground.selectSquare(key, true);
  };
  let usedSan = false;
  const cgState = root.chessground.state;
  return {
    drop(key, piece) {
      const role = (sanToRole as sanMap)[piece]
      const crazyData = root.data.crazyhouse
      const color = root.data.player.color
      // Piece not in Pocket
      if (!crazyData || !crazyData.pockets[color][role]) return
      // Square occupied
      if (root.chessground.state.pieces[key]) return
      if (!crazyValid(root.data, role, key)) return
      root.chessground.cancelMove();
      root.chessground.newPiece({ role, color }, key)
      root.sendNewPiece(role, key, false)
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
    hasSelected: () => root.chessground.state.selected,
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
              drop: ctrl.drop
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
