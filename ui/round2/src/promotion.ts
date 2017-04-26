import * as ground from './ground';
import * as cg from 'chessground/types';
import xhr = require('./xhr');
import { key2pos } from 'chessground/util';
import { bind } from './util';
import { h } from 'snabbdom'

let promoting: any | undefined;
let prePromotionRole: cg.Role | undefined;

function sendPromotion(ctrl, orig, dest, role, meta) {
  ground.promote(ctrl.chessground, dest, role);
  ctrl.sendMove(orig, dest, role, meta);
  return true;
}

export function start(ctrl, orig, dest, meta) {
  var d = ctrl.data;
  var piece = ctrl.chessground.state.pieces[dest];
  var premovePiece = ctrl.chessground.state.pieces[orig];
  if (((piece && piece.role === 'pawn') || (premovePiece && premovePiece.role === 'pawn')) && (
    (dest[1] == 8 && d.player.color === 'white') ||
      (dest[1] == 1 && d.player.color === 'black'))) {
    if (prePromotionRole && meta.premove) return sendPromotion(ctrl, orig, dest, prePromotionRole, meta);
    if (!meta.ctrlKey && (d.pref.autoQueen === 3 || (d.pref.autoQueen === 2 && premovePiece))) {
      if (premovePiece) setPrePromotion(ctrl, dest, 'queen');
      else sendPromotion(ctrl, orig, dest, 'queen', meta);
      return true;
    }
    promoting = {
      move: [orig, dest],
      pre: !!premovePiece,
      meta: meta
    };
    ctrl.redraw();
    return true;
  }
  return false;
}

function setPrePromotion(ctrl, dest, role) {
  prePromotionRole = role;
  ctrl.chessground.setAutoShapes([{
    orig: dest,
    piece: {
      color: ctrl.data.player.color,
      role: role,
      opacity: 0.8
    }
  }]);
}

export function cancelPrePromotion(ctrl) {
  if (prePromotionRole) {
    ctrl.chessground.setAutoShapes([]);
    prePromotionRole = undefined;
    ctrl.redraw();
  }
}

function finish(ctrl, role) {
  if (promoting) {
    if (promoting.pre) setPrePromotion(ctrl, promoting.move[1], role);
    else sendPromotion(ctrl, promoting.move[0], promoting.move[1], role, promoting.meta);
  }
  promoting = undefined;
}

export function cancel(ctrl) {
  cancelPrePromotion(ctrl);
  ctrl.chessground.cancelPremove();
  if (promoting) xhr.reload(ctrl).then(ctrl.reload);
  promoting = undefined;
}

function renderPromotion(ctrl, dest, pieces, color, orientation) {
  var left = (8 - key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;
  var vertical = color === orientation ? 'top' : 'bottom';

  return h('div#promotion_choice.' + vertical, {
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('click', () => cancel(ctrl));
        el.addEventListener('contextmenu', () => false);
      }
    }
  }, pieces.map((serverRole, i) => {
    var top = (color === orientation ? i : 7 - i) * 12.5;
    return h('square', {
      attrs: {style: 'top: ' + top + '%;left: ' + left + '%'},
      hook: bind('click', e => {
        e.stopPropagation();
        finish(ctrl, serverRole);
      })
    }, [
      h('piece.' + serverRole + '.' + color)
    ]);
  }));
};

export function view(ctrl) {
  if (!promoting) return;
  var pieces = ['queen', 'knight', 'rook', 'bishop'];
  if (ctrl.data.game.variant.key === 'antichess') pieces.push('king');

  return renderPromotion(ctrl, promoting.move[1], pieces,
    ctrl.data.player.color,
    ctrl.chessground.state.orientation);
};
