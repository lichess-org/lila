import { Role } from 'shogiops';
import { VNode, h } from 'snabbdom';
import { Selected, SpecialSelected, isSpecialSelected } from '../interfaces';
import { allRoles, promote } from 'shogiops/variant/util';
import EditorCtrl from '../ctrl';
import { MouchEvent } from 'shogiground/types';
import { eventPosition, samePiece } from 'shogiground/util';
import { dragNewPiece } from 'shogiground/drag';

export function sparePieces(ctrl: EditorCtrl, color: Color, position: 'top' | 'bottom'): VNode {
  const baseRoles: (Role | 'skip' | SpecialSelected)[] = [],
    promotedRoles: (Role | 'skip' | SpecialSelected)[] = [];

  // initial pieces first
  const roles: Role[] =
    ctrl.rules === 'kyotoshogi' ? ['pawn', 'gold', 'king', 'silver', 'tokin'] : allRoles(ctrl.rules);

  // baseRoles.push(position === 'bottom' ? 'pointer' : 'trash');
  // promotedRoles.push('skip');

  baseRoles.push('king');
  promotedRoles.push(position === 'bottom' ? 'pointer' : 'trash');
  roles.forEach(r => {
    if (r !== 'king') {
      if (!promotedRoles.includes(r)) {
        baseRoles.push(r);
        const promoted = promote(ctrl.rules)(r);
        if (promoted) {
          promotedRoles.push(promoted);
        } else promotedRoles.push('skip');
      }
    }
  });
  return h(
    'div.spare.spare-' + position + '.spare-' + color,
    {
      // so we don't force redraw because of class change
      hook: {
        update(_oldVNode: VNode, vnode: VNode) {
          const active = ctrl.shogiground.state.selectedPiece?.color === color,
            elm = vnode.elm as HTMLElement;
          elm.classList.toggle('active', active);
          if (!!ctrl.shogiground.state.selectedPiece && !active) elm.classList.toggle('inactive', !active);
          else elm.classList.remove('inactive');
        },
      },
    },
    ctrl.rules === 'chushogi'
      ? [
          h('div.outer', h('div.inner', sparesVNodes(ctrl, baseRoles.slice(0, 12), color))),
          h('div.outer', h('div.inner', sparesVNodes(ctrl, baseRoles.slice(12), color))),
          h('div.outer', h('div.inner', sparesVNodes(ctrl, promotedRoles.slice(0, 12), color))),
          h('div.outer', h('div.inner', sparesVNodes(ctrl, promotedRoles.slice(12), color))),
        ]
      : [
          h('div.outer', h('div.inner', sparesVNodes(ctrl, baseRoles, color))),
          h('div.outer', h('div.inner', sparesVNodes(ctrl, promotedRoles, color))),
        ]
  );
}

function sparesVNodes(ctrl: EditorCtrl, spares: (Role | 'skip' | SpecialSelected)[], color: Color): VNode[] {
  const selectedClass = selectedToClass(ctrl.selected());

  return spares.map((s: Role | 'skip' | SpecialSelected) => {
    if (s === 'skip') return h('div.selectable-wrap');
    const sel: Selected = isSpecialSelected(s) ? s : [color, s],
      className = selectedToClass(sel),
      attrs: Record<string, string> = {
        class: className,
      };

    if (typeof sel !== 'string') {
      attrs['data-color'] = s[0];
      attrs['data-role'] = s[1];
    }

    const selectableNode = h(typeof sel === 'string' ? 'div' : 'piece', { attrs });

    return h(
      'div.selectable-wrap',
      {
        class: {
          'selected-square': selectedClass === className,
        },
        on: {
          mousedown: selectSPStart(ctrl, sel),
          mouseup: selectSPEnd(ctrl, sel),
          touchstart: selectSPStart(ctrl, sel),
          touchend: selectSPEnd(ctrl, sel),
        },
      },
      selectableNode
    );
  });
}

// can be 'pointer', 'trash', or [color, role]
function selectedToClass(s: Selected): string {
  return typeof s === 'string' ? s : s.join(' ');
}

let initSpareEvent: Selected | undefined = undefined;
function selectSPStart(ctrl: EditorCtrl, s: Selected): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    initSpareEvent = s;
    e.preventDefault();
    ctrl.shogiground.selectPiece(null);
    ctrl.initTouchMovePos = ctrl.lastTouchMovePos = undefined;
    if (typeof s !== 'string') {
      const piece = {
        color: s[0],
        role: s[1],
      };
      ctrl.selected('pointer');
      dragNewPiece(ctrl.shogiground.state, piece, e, true);
    }
    ctrl.redraw();
  };
}

function selectSPEnd(ctrl: EditorCtrl, s: Selected): (e: MouchEvent) => void {
  return function (e: MouchEvent): void {
    if (!initSpareEvent) return;

    e.preventDefault();
    const cur = ctrl.selected(),
      pos = eventPosition(e) || ctrl.lastTouchMovePos;
    ctrl.shogiground.selectPiece(null);
    // default to pointer if we click on selected
    if (
      cur === s ||
      (typeof cur !== 'string' &&
        typeof s !== 'string' &&
        samePiece({ color: cur[0], role: cur[1] }, { color: s[0], role: s[1] }))
    ) {
      ctrl.selected('pointer');
    } else if (
      !pos ||
      !ctrl.initTouchMovePos ||
      (Math.abs(pos[0] - ctrl.initTouchMovePos[0]) < 20 && Math.abs(pos[1] - ctrl.initTouchMovePos[1]) < 20)
    ) {
      ctrl.selected(s);
      ctrl.shogiground.state.selectable.deleteOnTouch = s === 'trash' ? true : false;
      ctrl.shogiground.selectSquare(null);
    }
    initSpareEvent = undefined;
    ctrl.redraw();
  };
}

// maybe later, but it looks too cluttered
// // +/-
// export function handSpares(ctrl: EditorCtrl, position: 'top' | 'bottom'): VNode {
//   function getPiece(e: Event): Piece | undefined {
//     const role: Role | undefined = ((e.target as HTMLElement).dataset.role as Role) || undefined;
//     if (role) {
//       const color: Color =
//         (e.target as HTMLElement).dataset.pos === 'bottom'
//           ? ctrl.shogiground.state.orientation
//           : opposite(ctrl.shogiground.state.orientation);
//       return { role, color };
//     }
//     return;
//   }

//   const processPiece = (e: Event) => {
//     const piece = getPiece(e),
//       action = (e.target as HTMLElement).dataset.action;
//     if (piece) {
//       if (action === 'add') ctrl.addToHand(piece.color, piece.role, true);
//       else ctrl.removeFromHand(piece.color, piece.role, true);
//     }
//   };

//   const roleDiv: VNode[] = [];

//   const hRoles = handRoles(ctrl.rules);
//   if (position === 'bottom') hRoles.reverse();

//   for (const r of hRoles) {
//     roleDiv.push(
//       h('div', [
//         h('div.plus', { attrs: { 'data-role': r, 'data-pos': position, 'data-action': 'add' } }),
//         h('div.minus', { attrs: { 'data-role': r, 'data-pos': position } }),
//       ])
//     );
//   }
//   return h('div.hand-spare.hand-spare-' + position, { on: { click: () => processPiece } }, roleDiv);
// }
