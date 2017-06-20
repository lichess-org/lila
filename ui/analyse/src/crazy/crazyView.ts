import { drag } from './crazyCtrl';
import { h } from 'snabbdom'

const eventNames = ['mousedown', 'touchstart'];
const oKeys = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

export default function(ctrl, color, position) {
  if (!ctrl.vm.node.crazy) return;
  var pocket = ctrl.vm.node.crazy.pockets[color === 'white' ? 0 : 1];
  var dropped = ctrl.vm.justDropped;
  var captured = ctrl.vm.justCaptured;
  if (captured) {
    captured = captured.promoted ? 'pawn' : captured.role;
  }
  var activeColor = color === ctrl.turnColor();
  var usable = !ctrl.embed && activeColor;
  return h('div.pocket.is2d.' + position, {
    class: { usable },
    hook: {
      insert: vnode => {
        if (ctrl.embed) return;
        eventNames.forEach(name => {
          (vnode.elm as HTMLElement).addEventListener(name, e => drag(ctrl, color, e));
        });
      }
    }
  }, oKeys.map(role => {
    var nb = pocket[role] || 0;
    if (activeColor) {
      if (dropped === role) nb--;
      if (captured === role) nb++;
    }
    return h('piece.' + role + '.' + color, {
      attrs: {
        'data-role': role,
        'data-color': color,
        'data-nb': nb
      }
    });
  })
  );
}
