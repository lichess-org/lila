import { h } from 'snabbdom';
import { bind, onInsert } from './util';
import { Api as CgApi } from 'shogiground/api';
import * as cgUtil from 'shogiground/util';
import { Role } from 'shogiground/types';
import { MaybeVNode, Vm, Redraw, Promotion } from './interfaces';
import { Prop } from 'common';

export default function (vm: Vm, getGround: Prop<CgApi>, redraw: Redraw): Promotion {
  let promoting: any = false;

  function start(orig: Key, dest: Key, callback: (orig: Key, key: Key, prom?: Boolean) => void) {
    const g = getGround(),
      piece = g.state.pieces.get(dest);
    if (piece &&
        ["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(
          piece.role
        ) && (((["7", "8", "9"].includes(dest[1]) ||
        ["7", "8", "9"].includes(orig[1])) &&
        g.state.turnColor === "black") || // opposite color
        ((["1", "2", "3"].includes(dest[1]) ||
          ["1", "2", "3"].includes(orig[1])) &&
          g.state.turnColor === "white")
          )
    ) {
      promoting = {
        orig: orig,
        dest: dest,
        role: piece.role,
        callback: callback,
      };
      redraw();
      return true;
    }
    return false;
  }

  function promote(g: CgApi, key: Key, role: Role): void {
    const piece = g.state.pieces.get(key);
    if (piece && ["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(piece.role)) {
      g.setPieces(
        new Map([
          [
            key,
            {
              color: piece.color,
              role,
            },
          ],
        ])
      );
    }
  }

  function finish(role: Role): void {
    const promoted = !["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(role)
    if (promoting && promoted) promote(getGround(), promoting.dest, role);
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, promoted);
    promoting = false;
  }

  function cancel(): void {
    if (promoting) {
      promoting = false;
      getGround().set(vm.cgConfig);
      redraw();
    }
  }

  function renderPromotion(dest: Key, pieces: Role[], color: Color, orientation: Color): MaybeVNode {
    if (!promoting) return;

    let left = (8 - cgUtil.key2pos(dest)[0]) * 11.11 - cgUtil.key2pos(dest)[0] * 0.04;
    if (orientation === "white")
      left = cgUtil.key2pos(dest)[0] * 11.11 - cgUtil.key2pos(dest)[0] * 0.04;
    const vertical = color === orientation ? "top" : "bottom";

    return h(
      'div#promotion-choice.' + vertical,
      {
        hook: onInsert(el => {
          el.addEventListener('click', cancel);
          el.oncontextmenu = () => false;
        }),
      },
      pieces.map(function (serverRole, i) {
        let top = (color === orientation ? i : 8 - i) * 11.11 + 0.3;
        if (orientation === "white")
          top = (9 - (i + cgUtil.key2pos(dest)[1])) * 11.11 + 0.35;
        return h(
          'square',
          {
            attrs: {
              style: 'top: ' + top + '%;left: ' + left + '%',
            },
            hook: bind('click', e => {
              e.stopPropagation();
              finish(serverRole);
            }),
          },
          [h('piece.' + serverRole + '.' + color)]
        );
      })
    );
  }

  function promotesTo(role: Role | undefined): Role {
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

  return {
    start,
    cancel,
    view() {
      if (!promoting) return;

      const pieces: Role[] =
      getGround().state.turnColor === "white" ?
      [promotesTo(promoting.role), promoting.role] :
      [promoting.role, promotesTo(promoting.role)];

      return renderPromotion(
        promoting.dest,
        pieces,
        cgUtil.opposite(getGround().state.turnColor),
        getGround().state.orientation
      );
    },
  };
}
