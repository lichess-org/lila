import { drag, shadowDrop, selectToDrop } from "./crazyCtrl";
import { h } from "snabbdom";
import { MouchEvent } from "shogiground/types";
import { onInsert } from "../util";
import { Controller } from "../interfaces";
import { PocketRole } from "shogiops/types";
import { opposite } from "shogiops/util";


const eventNames1 = ["mousedown", "touchmove"];
const eventNames2 = ["click"];
const eventNames3 = ["contextmenu"];

const oKeys = ["pawn", "lance", "knight", "silver", "gold", "bishop", "rook"];

type Position = "top" | "bottom";

export default function (ctrl: Controller, position: Position) {
  const shogi = ctrl.position();
  // We are solving from the bottom, initial color is our color
  const color = position === "bottom" ? ctrl.vm.pov : opposite(ctrl.vm.pov);
  const pocket = shogi.pockets[color];

  const usable = color === shogi.turn;
  return h(
    `div.pocket.is2d.pocket-${position}`,
    {
      class: { usable },
      hook: onInsert((el) => {
        eventNames1.forEach((name) => {
          el.addEventListener(name, (e) => drag(ctrl, e as MouchEvent));
        });
        eventNames2.forEach((name) => {
          el.addEventListener(name, (e) => {
            selectToDrop(ctrl, color, e as MouchEvent);
          });
        });
        eventNames3.forEach((name) => {
          el.addEventListener(name, (e) => {
            shadowDrop(ctrl, color, e as MouchEvent);
          });
        });
      }),
    },
    oKeys.map((role) => {
      let nb = pocket[role as PocketRole] ?? 0;
      const selectedPiece = 
        role == ctrl.ground()?.state.drawable.piece?.role &&
        color == ctrl.ground()?.state.drawable.piece?.color;
      const selectedSquare: boolean = 
        !!ctrl.ground() && (
            ctrl.ground()!.state.dropmode.active && 
            ctrl.ground()?.state.dropmode.piece?.color === color &&
            ctrl.ground()?.state.dropmode.piece?.role === role &&
            ctrl.ground()?.state.movable.color == color);
      return h(
        "div.pocket-c1",
        h(
          "div.pocket-c2",
          {
            class: {
              "shadow-piece": selectedPiece,
            }
          },
          h("piece." + role + "." + color, {
            class: {
              "selected-square": selectedSquare,
            },
            attrs: {
              "data-role": role,
              "data-color": color,
              "data-nb": nb,
              cursor: "pointer"
            },
          })
        )
      );
    })
  );
}
