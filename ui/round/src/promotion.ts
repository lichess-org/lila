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
  meta: cg.MoveMetadata;
  role: cg.Role
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
  if (((piece && ['pawn', 'lance', 'knight', 'silver', 'bishop', 'rook'].includes(piece.role) && !premovePiece) ||
    (premovePiece && ['pawn', 'lance', 'knight', 'silver', 'bishop', 'rook'].includes(premovePiece.role))) && (
      ((['7', '8', '9'].includes(dest[1]) || ['7', '8', '9'].includes(orig[1])) && d.player.color === 'white') ||
      ((['1', '2', '3'].includes(dest[1]) || ['1', '2', '3'].includes(orig[1])) && d.player.color === 'black'))) {
    if (prePromotionRole && meta && meta.premove) return sendPromotion(ctrl, orig, dest, prePromotionRole, meta);
    if (!meta.ctrlKey && !promoting && (
      (ctrl.keyboardMove && ctrl.keyboardMove.justSelected()))) {
      if (premovePiece) setPrePromotion(ctrl, dest, 'lance');
      else sendPromotion(ctrl, orig, dest, 'lance', meta);
      return true;
    }
    const promotionRole = piece ? piece.role : (premovePiece ? premovePiece.role : 'pawn');
    promoting = {
      move: [orig, dest],
      pre: !!premovePiece,
      meta,
      role: promotionRole
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
  console.log("Enterd finish: ", role)
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
  console.log("dest promt: ", key2pos(dest))
  var left = (8 - key2pos(dest)[0]) * 11.11 - 0.29;
  if (orientation === 'white') left = (key2pos(dest)[0]) * 11.11;
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
    var top = (i + key2pos(dest)[1]) * 11.11 - 0.29;
    if (orientation === 'white') top = (9 - (i + key2pos(dest)[1])) * 11.11 - 0.29;
    //var top = (color === orientation ? i : 8 - i) * 11.33;
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

//const promotedRole: cg.Role[] = ['p_pawn', 'p_silver', 'p_knight', 'p_bishop', 'p_rook', 'p_lance']
//const roles: cg.Role[] = ['tokin', 'promotedSilver', 'promotedKnight', 'horse', 'dragon', 'promotedLance'];
//const roles: cg.Role[] = [];

function promotesTo(role: typeof prePromotionRole): cg.Role {
  switch (role) {
    case 'silver': return 'promotedSilver';
    case 'knight': return 'promotedKnight';
    case 'lance': return 'promotedLance';
    case 'bishop': return 'horse';
    case 'rook': return 'dragon';
    default: return 'tokin';
  }
}

export function view(ctrl: RoundController): MaybeVNode {
  if (!promoting) return;
  if ((promoting.role === 'pawn' || promoting.role === 'knight') && ['1', '2', '8', '9'].includes(promoting.move[1][1])) {
    console.log("Force promotion: ", promoting.move);
  }
  const roles: cg.Role[] = [promotesTo(promoting.role), promoting.role]
  return renderPromotion(ctrl, promoting.move[1],
    roles,
    ctrl.data.player.color,
    ctrl.shogiground.state.orientation);
}
