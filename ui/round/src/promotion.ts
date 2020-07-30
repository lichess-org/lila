import { h } from 'snabbdom'
import * as ground from './ground';
import * as cg from 'shogiground/types';
import { DrawShape } from 'shogiground/draw';
import * as xhr from './xhr';
import { key2pos } from 'shogiground/util';
import { bind } from './util';
import RoundController from './ctrl';
import { onInsert } from './util';
import { MaybeVNode } from './interfaces';

interface Promoting {
  move: [cg.Key, cg.Key];
  pre: boolean;
  meta: cg.MoveMetadata
}

let promoting: Promoting | undefined;
let prePromotionRole: cg.Role | undefined;

export function sendPromotion(ctrl: RoundController, orig: cg.Key, dest: cg.Key, role: cg.Role, meta: cg.MoveMetadata): boolean {
  ground.promote(ctrl.shogiground, dest, role);
  ctrl.sendMove(orig, dest, role, meta);
  return true;
}

export function start(ctrl: RoundController, orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata = {} as cg.MoveMetadata): boolean {
  const d = ctrl.data,
    piece = ctrl.shogiground.state.pieces.get(dest),
    premovePiece = ctrl.shogiground.state.pieces.get(orig);
  if (((piece && piece.role === 'pawn' && !premovePiece) || (premovePiece && premovePiece.role === 'pawn')) && (
    (dest[1] === '8' && d.player.color === 'white') ||
    (dest[1] === '1' && d.player.color === 'black'))) {
    if (prePromotionRole && meta && meta.premove) return sendPromotion(ctrl, orig, dest, prePromotionRole, meta);
    if (!meta.ctrlKey && !promoting && (
      d.pref.autoQueen === 3 ||
      (d.pref.autoQueen === 2 && premovePiece) ||
      (ctrl.keyboardMove && ctrl.keyboardMove.justSelected()))) {
      if (premovePiece) setPrePromotion(ctrl, dest, 'lance');
      else sendPromotion(ctrl, orig, dest, 'lance', meta);
      return true;
    }
    promoting = {
      move: [orig, dest],
      pre: !!premovePiece,
      meta
    };
    ctrl.redraw();
    return true;
  }
  return false;
}

function setPrePromotion(ctrl: RoundController, dest: cg.Key, role: cg.Role): void {
  prePromotionRole = role;
  ctrl.shogiground.setAutoShapes([{
    orig: dest,
    piece: {
      color: ctrl.data.player.color,
      role,
      opacity: 0.8
    },
    brush: ''
  } as DrawShape]);
}

export function cancelPrePromotion(ctrl: RoundController) {
  if (prePromotionRole) {
    ctrl.shogiground.setAutoShapes([]);
    prePromotionRole = undefined;
    ctrl.redraw();
  }
}

function finish(ctrl: RoundController, role: cg.Role) {
  if (promoting) {
    const info = promoting;
    promoting = undefined;
    if (info.pre) setPrePromotion(ctrl, info.move[1], role);
    else sendPromotion(ctrl, info.move[0], info.move[1], role, info.meta);
    ctrl.redraw();
  }
}

export function cancel(ctrl: RoundController) {
  cancelPrePromotion(ctrl);
  ctrl.shogiground.cancelPremove();
  if (promoting) xhr.reload(ctrl).then(ctrl.reload);
  promoting = undefined;
}

function renderPromotion(ctrl: RoundController, dest: cg.Key, roles: cg.Role[], color: Color, orientation: Color): MaybeVNode {
  var left = (7 - key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;
  var vertical = color === orientation ? 'top' : 'bottom';

  return h('div#promotion-choice.' + vertical, {
    hook: onInsert(el => {
      el.addEventListener('click', () => cancel(ctrl));
      el.addEventListener('contextmenu', e => {
        e.preventDefault();
        return false;
      });
    })
  }, roles.map((serverRole, i) => {
    var top = (color === orientation ? i : 7 - i) * 12.5;
    return h('square', {
      attrs: {
        style: `top:${top}%;left:${left}%`
      },
      hook: bind('click', e => {
        e.stopPropagation();
        finish(ctrl, serverRole);
      })
    }, [
      h(`piece.${serverRole}.${color}`)
    ]);
  }));
}

const roles: cg.Role[] = ['lance', 'knight', 'rook', 'bishop'];

export function view(ctrl: RoundController): MaybeVNode {
  if (!promoting) return;

  return renderPromotion(ctrl, promoting.move[1],
    ctrl.data.game.variant.key === 'antichess' ? roles.concat('king') : roles,
    ctrl.data.player.color,
    ctrl.shogiground.state.orientation);
}
