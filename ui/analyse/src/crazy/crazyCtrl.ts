import { dragNewPiece } from "shogiground/drag";
import { readDrops } from "chess";
import AnalyseCtrl from "../ctrl";
import * as cg from "shogiground/types";
import { Api as ShogigroundApi } from "shogiground/api";

// @ts-ignore
import { Shogi } from "shogiutil/vendor/Shogi.js";

export function drag(ctrl: AnalyseCtrl, color: Color, e: cg.MouchEvent): void {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.shogiground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute("data-role") as cg.Role,
    number = el.getAttribute("data-nb");
  if (!role || !color || number === "0") return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.shogiground.state, { color, role }, e);
}

export function selectToDrop(ctrl: AnalyseCtrl, color: Color, e: cg.MouchEvent) : void {
  if (e.button !== undefined && e.button !== 0) return;
  if (ctrl.shogiground.state.movable.color !== color) return;
  const el = e.target as HTMLElement;
  const role = el.getAttribute("data-role") as cg.Role,
    number = el.getAttribute("data-nb");
  if (!role || !color || number === "0") return;
  if(!ctrl.selected || ctrl.selected[1] !== role){
    ctrl.selected = [color, role];
  }
  else{
    ctrl.selected = undefined;
  }
  ctrl.redraw();
}

export function valid(
  shogiground: ShogigroundApi,
  possibleDrops: string | undefined | null,
  piece: cg.Piece,
  pos: Key
): boolean {
  if (piece.color !== shogiground.state.movable.color) return false;

  // You can't place pawn on a file where you already have a pawn
  if (piece.role === "pawn") {
    for (const [k, v] of shogiground.state.pieces.entries()) {
      if (
        v.role === "pawn" &&
        v.color === piece.color &&
        pos[0] === k[0] &&
        pos != k
      ) {
        return false;
      }
    }
  }

  if (
    (piece.role === "pawn" || piece.role === "lance") &&
    ((pos[1] === "1" && piece.color === "black") ||
      (pos[1] === "9" && piece.color === "white"))
  )
    return false;
  if (
    piece.role === "knight" &&
    (((pos[1] === "1" || pos[1] === "2") && piece.color === "black") ||
      ((pos[1] === "9" || pos[1] === "8") && piece.color === "white"))
  )
    return false;

  const drops = readDrops(possibleDrops);

  if (drops === null) return true;

  return drops.includes(pos);
}
