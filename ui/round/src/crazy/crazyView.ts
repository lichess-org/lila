import { h } from "snabbdom";
import * as round from "../round";
import { drag, crazyKeys, pieceRoles, selectToDrop } from "./crazyCtrl";
import * as cg from "shogiground/types";
import RoundController from "../ctrl";
import { onInsert } from "../util";
import { Position } from "../interfaces";

const eventNames1 = ["mousedown", "touchmove"];
const eventNames2 = ["click"];

export default function pocket(
  ctrl: RoundController,
  color: Color,
  position: Position
) {
  const step = round.plyStep(ctrl.data, ctrl.ply);
  if (!step.crazy) {
    return;
  }
  const droppedRole = ctrl.justDropped,
    preDropRole = ctrl.preDrop,
    pocket = step.crazy.pockets[color === "white" ? 0 : 1],
    usablePos = position === (ctrl.flip ? "top" : "bottom"),
    usable = usablePos && !ctrl.replaying() && ctrl.isPlaying(),
    activeColor = color === ctrl.data.player.color;
  const capturedPiece = ctrl.justCaptured;
  const captured =
    capturedPiece &&
    (!capturedPiece["promoted"]
      ? capturedPiece.role
      : capturedPiece.role === "tokin"
      ? "pawn"
      : capturedPiece.role === "promotedLance"
      ? "lance"
      : capturedPiece.role === "promotedKnight"
      ? "knight"
      : capturedPiece.role === "promotedSilver"
      ? "silver"
      : capturedPiece.role === "horse"
      ? "bishop"
      : "rook");
  return h(
    "div.pocket.is2d.pocket-" + position,
    {
      class: { usable },
      hook: onInsert((el) => {
        eventNames1.forEach((name) =>
          el.addEventListener(name, (e: cg.MouchEvent) => {
            if (
              position === (ctrl.flip ? "top" : "bottom") &&
              crazyKeys.length == 0
            )
              drag(ctrl, e);
          })
        );
        eventNames2.forEach((name) =>
          el.addEventListener(name, (e: cg.MouchEvent) => {
            if (
              position === (ctrl.flip ? "top" : "bottom") &&
              crazyKeys.length == 0
            )
              selectToDrop(ctrl, e);
          })
        );
      }),
    },
    pieceRoles.map((role) => {
      let nb = pocket[role] || 0;
      const selectedSquare : boolean = (!!ctrl.selected && ctrl.selected[0] === color && ctrl.selected[1] === role);
      if (activeColor) {
        if (droppedRole === role) nb--;
        if (captured === role) nb++;
      }
      return h(
        "div.pocket-c1",
        h(
          "div.pocket-c2",
          h("piece." + role + "." + color, {
            class: { premove: activeColor && preDropRole === role, "selected-square": selectedSquare },
            attrs: {
              "data-role": role,
              "data-color": color,
              "data-nb": nb,
            },
          })
        )
      );
    })
  );
}
