import { Piece, Role } from 'shogiops/types';
import { VNode, h } from 'snabbdom';
import { Selected, SpecialSelected, isSpecialSelected } from '../interfaces';
import { allRoles, promote } from 'shogiops/variant/util';
import EditorCtrl from '../ctrl';
import { MouchEvent } from 'shogiground/types';
import { eventPosition } from 'shogiground/util';
import { dragNewPiece } from 'shogiground/drag';

export function sparePieces(ctrl: EditorCtrl, color: Color, position: 'top' | 'bottom'): VNode {
  const baseRoles: (Role | 'skip' | SpecialSelected)[] = [],
    promotedRoles: (Role | 'skip' | SpecialSelected)[] = [];

  // initial pieces first
  const roles: Role[] =
    ctrl.rules === 'kyotoshogi' ? ['pawn', 'gold', 'king', 'silver', 'tokin'] : allRoles(ctrl.rules);

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
    'div.spare.spare-' + position + '.spare-' + color + '.v-' + ctrl.rules,
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
      pos = eventPosition(e) || ctrl.lastTouchMovePos,
      dragged = ctrl.shogiground.state.draggable.current?.piece as Piece | undefined;
    ctrl.shogiground.selectPiece(null);
    // default to pointer if we click on selected
    if (compareSelected(cur, s)) {
      ctrl.selected('pointer');
    } else if (
      (!dragged || compareSelected([dragged.color, dragged.role], s)) &&
      (!pos ||
        !ctrl.initTouchMovePos ||
        (Math.abs(pos[0] - ctrl.initTouchMovePos[0]) < 20 && Math.abs(pos[1] - ctrl.initTouchMovePos[1]) < 20))
    ) {
      ctrl.selected(s);
      ctrl.shogiground.state.selectable.deleteOnTouch = s === 'trash' ? true : false;
      ctrl.shogiground.selectSquare(null);
    }
    initSpareEvent = undefined;
    ctrl.redraw();
  };
}

function compareSelected(a: Selected, b: Selected): boolean {
  if (typeof a === 'string') return a === b;
  else return a[0] === b[0] && a[1] === b[1];
}

// maybe later, but it looks too cluttered
// +/-
// export function handSpares(ctrl: EditorCtrl, position: 'top' | 'bottom'): VNode {
//   const processPiece = (e: Event) => {
//     const piece = getPiece(e, ctrl.shogiground.state.orientation),
//       action = (e.target as HTMLElement).dataset.action;
//     console.log(e, piece, action);
//     if (piece) {
//       if (action === 'add') ctrl.addToHand(piece.color, piece.role, true);
//       else ctrl.removeFromHand(piece.color, piece.role, true);
//     }
//   };

//   const hRoles = handRoles(ctrl.rules);
//   if (position === 'bottom') hRoles.reverse();

//   return h(
//     'div.hand-spare.hand-spare-' + position,
//     { on: { click: () => processPiece } },
//     hRoles.map(r =>
//       h('div', [
//         h('div.plus', { attrs: { 'data-role': r, 'data-pos': position, 'data-action': 'add' } }, '+'),
//         h('div.minus', { attrs: { 'data-role': r, 'data-pos': position } }, '-'),
//       ])
//     )
//   );
// }

// function getPiece(e: Event, orientation: Color): Piece | undefined {
//   const role: Role | undefined = ((e.target as HTMLElement).dataset.role as Role) || undefined;
//   if (role) {
//     const color: Color = (e.target as HTMLElement).dataset.pos === 'bottom' ? orientation : opposite(orientation);
//     return { role, color };
//   }
//   return;
// }
