import { h } from 'snabbdom'
import * as ground from './ground';
import { bind } from './util';
import * as util from 'chessground/util';

let promoting: any = false;

export function start(ctrl, orig, dest, capture, callback) {
  var s = ctrl.chessground.state;
  var piece = s.pieces[dest];
  if (piece && piece.role == 'pawn' && (
    (dest[1] == 8 && s.turnColor == 'black') ||
      (dest[1] == 1 && s.turnColor == 'white'))) {
    promoting = {
      orig: orig,
      dest: dest,
      capture: capture,
      callback: callback
    };
    ctrl.redraw();
  return true;
  }
  return false;
}

function finish(ctrl, role) {
  if (promoting) ground.promote(ctrl.chessground, promoting.dest, role);
  if (promoting.callback) promoting.callback(promoting.orig, promoting.dest,
    promoting.capture, role);
  promoting = false;
}

export function cancel(ctrl) {
  if (promoting) {
    promoting = false;
    ctrl.chessground.set(ctrl.vm.cgConfig);
    ctrl.redraw();
  }
}

function renderPromotion(ctrl, dest, pieces, color, orientation) {
  if (!promoting) return;

  var left = (8 - util.key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;

  var vertical = color === orientation ? 'top' : 'bottom';

  return h('div#promotion_choice.' + vertical, {
    hook: {
      insert: vnode => {
        const el = (vnode.elm as HTMLElement);
        el.addEventListener('click', _ => cancel(ctrl));
        el.addEventListener('contextmenu', _ => false);
      }
    }
  }, pieces.map(function(serverRole, i) {
    const top = (color === orientation ? i : 7 - i) * 12.5;
    return h('square', {
      attrs: {
        style: 'top: ' + top + '%;left: ' + left + '%'
      },
      hook: bind('click', e => {
        e.stopPropagation();
        finish(ctrl, serverRole);
      })
    }, [h('piece.' + serverRole + '.' + color)]);
  }));
}

export function view(ctrl) {
  if (!promoting) return;
  var pieces = ['queen', 'knight', 'rook', 'bishop'];
  if (ctrl.data.game.variant.key === "antichess") pieces.push('king');

  return renderPromotion(ctrl, promoting.dest, pieces,
    util.opposite(ctrl.chessground.state.turnColor),
    ctrl.chessground.state.orientation);
}
