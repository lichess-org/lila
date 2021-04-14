import { drag, selectToDrop, shadowDrop } from "./crazyCtrl";
import { h } from "snabbdom";
import { MouchEvent } from "shogiground/types";
import { onInsert } from "../util";
import AnalyseCtrl from "../ctrl";

const eventNames1 = ["mousedown", "touchmove"];
const eventNames2 = ["click"];
const eventNames3 = ["contextmenu"];
const oKeys = ["pawn", "lance", "knight", "silver", "gold", "bishop", "rook"];

type Position = "top" | "bottom";

export default function (ctrl: AnalyseCtrl, color: Color, position: Position) {
  if (!ctrl.node.crazy) return;
  const pocket = ctrl.node.crazy.pockets[color === "white" ? 0 : 1];
  const dropped = ctrl.justDropped;
  const shadowPiece = ctrl.shogiground?.state.drawable.piece;
  let captured = ctrl.justCaptured;

  if (captured)
    captured.role =
      captured &&
      (!captured["promoted"]
        ? captured.role
        : captured.role === "tokin"
          ? "pawn"
          : captured.role === "promotedLance"
            ? "lance"
            : captured.role === "promotedKnight"
              ? "knight"
              : captured.role === "promotedSilver"
                ? "silver"
                : captured.role === "horse"
                  ? "bishop"
                  : "rook");

  const activeColor = color === ctrl.turnColor();
  const usable = !ctrl.embed && activeColor;
  return h(
    `div.pocket.is2d.pocket-${position}.pos-${ctrl.bottomColor()}`,
    {
      class: { usable },
      hook: onInsert((el) => {
        if (ctrl.embed) return;
        eventNames1.forEach((name) => {
          el.addEventListener(name, (e) => drag(ctrl, color, e as MouchEvent));
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
      let nb = pocket[role] || 0;
      const sp = (role == shadowPiece?.role && color == shadowPiece?.color);
      const selectedSquare: boolean = (ctrl.shogiground?.state.dropmode.active &&
        ctrl.shogiground?.state.dropmode.piece?.color === color &&
        ctrl.shogiground?.state.dropmode.piece?.role === role &&
        ctrl.shogiground.state.movable.color === ctrl.shogiground?.state.dropmode.piece.color);
      if (activeColor) {
        if (dropped === role) nb--;
        if (captured && captured.role === role) nb++;
      }
      return h(
        "div.pocket-c1",
        h(
          "div.pocket-c2",
          {
            class: {
              "shadow-piece": sp,
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
