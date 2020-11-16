import { h } from "snabbdom";
import * as cg from "shogiground/types";
import { Step, Redraw } from "./interfaces";
import RoundController from "./ctrl";
import { ClockController } from "./clock/clockCtrl";
import { valid as crazyValid } from "./crazy/crazyCtrl";
import { sendPromotion } from "./promotion";
import { onInsert } from "./util";

export type KeyboardMoveHandler = (
  fen: Fen,
  dests?: cg.Dests,
  yourMove?: boolean
) => void;

export interface KeyboardMove {
  drop(key: cg.Key, piece: string): void;
  promote(orig: cg.Key, dest: cg.Key, piece: string): void;
  update(step: Step, yourMove?: boolean): void;
  registerHandler(h: KeyboardMoveHandler): void;
  hasFocus(): boolean;
  setFocus(v: boolean): void;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  confirmMove(): void;
  usedSan: boolean;
  jump(delta: number): void;
  justSelected(): boolean;
  clock(): ClockController | undefined;
}

const sanToRole: { [key: string]: cg.Role } = {
  P: "pawn",
  L: "lance",
  N: "knight",
  S: "silver",
  G: "gold",
  B: "bishop",
  R: "rook",
  K: "king",
  H: "horse",
  D: "dragon",
};

export function ctrl(
  root: RoundController,
  step: Step,
  redraw: Redraw
): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  let lastSelect = Date.now();
  const cgState = root.shogiground.state;
  const select = function (key: cg.Key): void {
    if (cgState.selected === key) root.shogiground.cancelMove();
    else {
      root.shogiground.selectSquare(key, true);
      lastSelect = Date.now();
    }
  };
  let usedSan = false;
  return {
    drop(key, piece) {
      const role = sanToRole[piece];
      const crazyData = root.data.crazyhouse;
      const color = root.data.player.color;
      // Square occupied
      if (!role || !crazyData || cgState.pieces[key]) return;
      // Piece not in Pocket
      if (!crazyData.pockets[color === "white" ? 0 : 1][role]) return;
      if (!crazyValid(root.data, root, role, key)) return;
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
      if (handler) handler(step.fen, cgState.movable.dests, yourMove);
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
      root.shogiground.cancelMove();
      select(orig);
      select(dest);
    },
    select,
    hasSelected: () => cgState.selected,
    confirmMove() {
      root.submitMove(true);
    },
    usedSan,
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
  return h("div.keyboard-move", [
    h("input", {
      attrs: {
        spellcheck: false,
        autocomplete: false,
      },
      hook: onInsert((el) => {
        window.lishogi
          .loadScript("compiled/lishogi.round.keyboardMove.min.js")
          .then(() => {
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
      ? h("em", "Enter SAN (Nc3) or UCI (b1c3) moves, or type / to focus chat")
      : h("strong", "Press <enter> to focus"),
  ]);
}
