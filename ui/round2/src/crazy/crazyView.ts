import * as round from '../round';
import { drag } from './crazyCtrl';
import { game } from 'game';

import { h } from 'snabbdom'

const eventNames = ['mousedown', 'touchstart'];
const pieceRoles = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

export default function pocket(ctrl, color, position) {
  var step = round.plyStep(ctrl.data, ctrl.vm.ply);
  if (!step.crazy) return;
  var droppedRole = ctrl.vm.justDropped;
  var preDropRole = ctrl.vm.preDrop;
  var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
  var usablePos = position === (ctrl.vm.flip ? 'top' : 'bottom');
  var usable = usablePos && !ctrl.replaying() && game.isPlayerPlaying(ctrl.data);
  var activeColor = color === ctrl.data.player.color;
  var captured = ctrl.vm.justCaptured;
  if (captured) captured = captured.promoted ? 'pawn' : captured.role;
  return h('div.pocket.is2d.' + position, {
    class: { usable },
    hook: {
      insert: vnode => {
        eventNames.forEach(name => {
          (vnode.elm as HTMLElement).addEventListener(name, e => {
            if (position === (ctrl.vm.flip ? 'top' : 'bottom')) drag(ctrl, e);
          })
        });
      }
    }
  }, pieceRoles.map(role => {
    var nb = pocket[role] || 0;
    if (activeColor) {
      if (droppedRole === role) nb--;
      if (captured === role) nb++;
    }
    return h('piece.' + role + '.' + color, {
      class: { premove: activeColor && preDropRole === role },
      attrs: {
        'data-role': role,
        'data-color': color,
        'data-nb': nb,
      }
    });
  }));
};
