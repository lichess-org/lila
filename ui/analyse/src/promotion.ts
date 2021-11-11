import { h } from 'snabbdom';
import * as ground from './ground';
import { bind, onInsert } from './util';
import * as util from 'shogiground/util';
import { Role } from 'shogiground/types';
import AnalyseCtrl from './ctrl';
import { MaybeVNode, JustCaptured } from './interfaces';
import { lishogiVariantRules, parseChessSquare } from 'shogiops/compat';
import { pieceCanPromote, promote } from 'shogiops/variantUtil';

interface Promoting {
  orig: Key;
  dest: Key;
  capture?: JustCaptured;
  role: Role;
  callback: Callback;
}

type Callback = (orig: Key, dest: Key, capture: JustCaptured | undefined, promotion: Boolean) => void;

let promoting: Promoting | undefined;

export function start(
  ctrl: AnalyseCtrl,
  orig: Key,
  dest: Key,
  capture: JustCaptured | undefined,
  callback: Callback
): boolean {
  const s = ctrl.shogiground.state;
  const piece = s.pieces.get(dest);
  if (!piece) return false;
  if (
    pieceCanPromote(lishogiVariantRules(ctrl.data.game.variant.key))(
      piece,
      parseChessSquare(orig)!,
      parseChessSquare(dest)!
    )
  ) {
    promoting = {
      orig: orig,
      dest: dest,
      capture: capture,
      role: piece.role,
      callback,
    };
    ctrl.redraw();
    return true;
  }
  return false;
}

function finish(ctrl: AnalyseCtrl, role: Role): void {
  const promoted = !['pawn', 'lance', 'knight', 'silver', 'bishop', 'rook'].includes(role);
  if (promoting && promoted) ground.promote(ctrl.shogiground, promoting.dest, role);
  if (promoting && promoting.callback) promoting.callback(promoting.orig, promoting.dest, promoting.capture, promoted);
  promoting = undefined;
}

export function cancel(ctrl: AnalyseCtrl): void {
  if (promoting) {
    promoting = undefined;
    ctrl.shogiground.set(ctrl.cgConfig);
    ctrl.redraw();
  }
}

function renderPromotion(ctrl: AnalyseCtrl, dest: Key, pieces: string[], color: Color, orientation: Color): MaybeVNode {
  if (!promoting) return;
  const numOfFiles = ctrl.shogiground.state.dimensions.files;
  let left = (numOfFiles - 1 - util.key2pos(dest)[0]) * (100 / numOfFiles);
  if (orientation === 'gote') left = util.key2pos(dest)[0] * (100 / numOfFiles);

  const vertical = color === orientation ? 'top' : 'bottom';

  return h(
    'div#promotion-choice.' + vertical,
    {
      hook: onInsert(el => {
        el.addEventListener('click', _ => cancel(ctrl));
        el.oncontextmenu = () => false;
      }),
    },
    pieces.map(function (serverRole: Role, i) {
      const numOfRanks = ctrl.shogiground.state.dimensions.ranks;
      let top = (i + util.key2pos(dest)[1]) * (100 / numOfRanks);
      if (orientation === 'gote') top = (numOfRanks - (i + util.key2pos(dest)[1])) * (100 / numOfRanks);
      return h(
        'square',
        {
          attrs: {
            style: `top:${top}%;left:${left}%;`,
          },
          hook: bind('click', e => {
            e.stopPropagation();
            finish(ctrl, serverRole);
          }),
        },
        [h(`piece.${serverRole}.${color}`)]
      );
    })
  );
}

export function view(ctrl: AnalyseCtrl): MaybeVNode {
  if (!promoting) return;

  const roles: Role[] =
    ctrl.shogiground.state.orientation === 'sente'
      ? [promote(lishogiVariantRules(ctrl.data.game.variant.key))(promoting.role), promoting.role]
      : [promoting.role, promote(lishogiVariantRules(ctrl.data.game.variant.key))(promoting.role)];

  return renderPromotion(ctrl, promoting.dest, roles, ctrl.turnColor(), ctrl.shogiground.state.orientation);
}
