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

    const selectableNode =
      typeof sel === 'string' ? h('div', { ...attrs, props: { innerHtml: svgIcons(sel) } }) : h('piece', { attrs });

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

function svgIcons(s: SpecialSelected): string {
  if (s === 'pointer')
    return '<svg xmlns="http://www.w3.org/2000/svg" xml:space="preserve" viewBox="0 0 297 297"><path d="M244.279 91.662a29.65 29.65 0 0 0-11.825 2.45c-4.413-11.152-15.238-19.058-27.869-19.058a29.65 29.65 0 0 0-13.094 3.034c-5.009-9.657-15.048-16.27-26.598-16.27-3.395 0-6.655.579-9.701 1.632V30.201C155.193 13.549 141.738 0 125.198 0 108.66 0 95.206 13.549 95.206 30.201v119.643L73.604 125.13a12 12 0 0 0-.465-.494c-5.672-5.676-13.221-8.823-21.256-8.862h-.153c-8.016 0-15.521 3.095-21.146 8.724-9.918 9.921-10.467 24.647-1.502 40.408 11.605 20.39 24.22 39.616 35.351 56.581 8.134 12.398 15.818 24.108 21.435 33.79 4.871 8.402 17.801 35.651 17.933 35.926a10.14 10.14 0 0 0 9.163 5.798h128.27c4.407 0 8.308-2.843 9.659-7.035 2.392-7.439 23.379-73.398 23.379-98.871v-69.229c-.002-16.656-13.455-30.204-29.993-30.204m-9.7 30.203c0-5.468 4.352-9.916 9.7-9.916 5.351 0 9.703 4.448 9.703 9.916v69.229c0 16.928-13.01 62.437-20.189 85.618H119.361c-4.206-8.752-12.089-24.964-15.944-31.613-5.897-10.168-13.73-22.105-22.022-34.744-10.966-16.71-23.393-35.652-34.681-55.482-2.946-5.181-5.646-12.166-1.78-16.032 1.803-1.807 4.231-2.751 6.851-2.779a9.9 9.9 0 0 1 6.805 2.721l39.124 44.755a10.144 10.144 0 0 0 17.781-6.676V30.201c0-5.467 4.353-9.913 9.704-9.913s9.706 4.446 9.706 9.913v94.711c0 5.602 4.543 10.144 10.144 10.144s10.144-4.542 10.144-10.144V92.016c0-5.464 4.352-9.909 9.701-9.909 5.351 0 9.703 4.445 9.703 9.909v46.127c0 5.605 4.542 10.145 10.143 10.145s10.145-4.539 10.145-10.145v-32.888c0-5.467 4.352-9.914 9.701-9.914 5.352 0 9.706 4.447 9.706 9.914v46.13c0 5.601 4.542 10.145 10.144 10.145s10.145-4.544 10.145-10.145v-29.52z"/></svg>';
  else
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path fill-opacity=".01" d="M0 0h512v512H0z"/><path d="M199 103v50h-78v30h270v-30h-78v-50zm18 18h78v32h-78zm-79.002 80 30.106 286h175.794l30.104-286zm62.338 13.38.64 8.98 16 224 .643 8.976-17.956 1.283-.64-8.98-16-224-.643-8.976zm111.328 0 17.955 1.284-.643 8.977-16 224-.64 8.98-17.956-1.284.643-8.977 16-224 .64-8.98zM247 215h18v242h-18z"/></svg>';
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
