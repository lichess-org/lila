import { h } from "snabbdom";
import * as ground from "./ground";
import { bind, onInsert } from "./util";
import * as util from "shogiground/util";
import { Role } from "shogiground/types";
import AnalyseCtrl from "./ctrl";
import { MaybeVNode, JustCaptured } from "./interfaces";

interface Promoting {
  orig: Key;
  dest: Key;
  capture?: JustCaptured;
  role: Role;
  callback: Callback;
}

type Callback = (
  orig: Key,
  dest: Key,
  capture: JustCaptured | undefined,
  role: Boolean
) => void;

let promoting: Promoting | undefined;

export function start(
  ctrl: AnalyseCtrl,
  orig: Key,
  dest: Key,
  capture: JustCaptured | undefined,
  callback: Callback
): boolean {
  const s = ctrl.shogiground.state;
  const piece = s.pieces.get(dest);
  const role = piece ? piece.role : "pawn";
  if (
    piece &&
    ["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(
      piece.role
    ) &&
    (((["7", "8", "9"].includes(dest[1]) ||
      ["7", "8", "9"].includes(orig[1])) &&
      s.turnColor === "black") ||
      ((["1", "2", "3"].includes(dest[1]) ||
        ["1", "2", "3"].includes(orig[1])) &&
        s.turnColor === "white"))
  ) {
    promoting = {
      orig,
      dest,
      capture,
      role,
      callback,
    };
    ctrl.redraw();
    return true;
  }
  return false;
}

function finish(ctrl: AnalyseCtrl, role: Role): void {
  if (promoting) {
    ground.promote(ctrl.shogiground, promoting.dest, role);
    let prom: boolean = false;
    if (!["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(role))
      prom = true;
    if (promoting.callback)
      promoting.callback(
        promoting.orig,
        promoting.dest,
        promoting.capture,
        prom
      );
  }
  promoting = undefined;
}

export function cancel(ctrl: AnalyseCtrl): void {
  if (promoting) {
    promoting = undefined;
    ctrl.shogiground.set(ctrl.cgConfig);
    ctrl.redraw();
  }
}

function renderPromotion(
  ctrl: AnalyseCtrl,
  dest: Key,
  pieces: string[],
  color: Color,
  orientation: Color
): MaybeVNode {
  if (!promoting) return;

  let left = (8 - util.key2pos(dest)[0]) * 11.11 - util.key2pos(dest)[0] * 0.04;
  if (orientation === "white")
    left = util.key2pos(dest)[0] * 11.11 - util.key2pos(dest)[0] * 0.04;

  const vertical = color === orientation ? "top" : "bottom";

  return h(
    "div#promotion-choice." + vertical,
    {
      hook: onInsert((el) => {
        el.addEventListener("click", (_) => cancel(ctrl));
        el.oncontextmenu = () => false;
      }),
    },
    pieces.map(function (serverRole: Role, i) {
      let top = (i + util.key2pos(dest)[1]) * 11.11 + 0.3;
      if (orientation === "white")
        top = (9 - (i + util.key2pos(dest)[1])) * 11.11 + 0.35;
      return h(
        "square",
        {
          attrs: {
            style: `top:${top}%;left:${left}%;display:table;`,
          },
          hook: bind("click", (e) => {
            e.stopPropagation();
            finish(ctrl, serverRole);
          }),
        },
        [h(`piece.${serverRole}.${color}`)]
      );
    })
  );
}

function promotesTo(role: Role): Role {
  switch (role) {
    case "silver":
      return "promotedSilver";
    case "knight":
      return "promotedKnight";
    case "lance":
      return "promotedLance";
    case "bishop":
      return "horse";
    case "rook":
      return "dragon";
    default:
      return "tokin";
  }
}

export function view(ctrl: AnalyseCtrl): MaybeVNode {
  if (!promoting) return;

  const roles: Role[] =
    ctrl.shogiground.state.orientation === "black" ?
    [promotesTo(promoting.role), promoting.role] :
    [promoting.role, promotesTo(promoting.role)];
  
  return renderPromotion(
    ctrl,
    promoting.dest,
    roles,
    promoting.dest[1] >= "7" ? "white" : "black",
    ctrl.shogiground.state.orientation
  );
}
