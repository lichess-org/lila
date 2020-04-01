import { h } from 'snabbdom'
import { bind, onInsert } from './util';
import * as cgUtil from 'chessground/util';
import { Role } from 'chessground/types';
import { MaybeVNode, Vm, Redraw, Promotion } from './interfaces';

export default function(vm: Vm, getGround, redraw: Redraw): Promotion {

  let promoting: any = false;

  function start(orig: Key, dest: Key, callback: (orig: Key, key: Key, prom: Role) => void) {
    const g = getGround(),
    piece = g.state.pieces[dest];
    if (piece && piece.role == 'pawn' && (
      (dest[1] == '8' && g.state.turnColor == 'black') ||
        (dest[1] == '1' && g.state.turnColor == 'white'))) {
      promoting = {
        orig: orig,
        dest: dest,
        callback: callback
      };
      redraw();
    return true;
    }
    return false;
  }

  function promote(g, key: Key, role: Role): void {
    var pieces = {};
    var piece = g.state.pieces[key];
    if (piece && piece.role == 'pawn') {
      pieces[key] = {
        color: piece.color,
        role: role,
        promoted: true
      };
      g.setPieces(pieces);
    }
  }

  function finish(role: Role): void {
    if (promoting) promote(getGround(), promoting.dest, role);
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
    promoting = false;
  }

  function cancel(): void {
    if (promoting) {
      promoting = false;
      getGround().set(vm.cgConfig);
      redraw();
    }
  }

  function renderPromotion(dest: Key, pieces: Role[], color: Color, orientation: Color): MaybeVNode {
    if (!promoting) return;

    let left = (8 - cgUtil.key2pos(dest)[0]) * 12.5;
    if (orientation === 'white') left = 87.5 - left;

    const vertical = color === orientation ? 'top' : 'bottom';

    return h('div#promotion-choice.' + vertical, {
      hook: onInsert(el => {
          el.addEventListener('click', cancel);
          el.oncontextmenu = () => false;
      })
    }, pieces.map(function(serverRole, i) {
      const top = (color === orientation ? i : 7 - i) * 12.5;
      return h('square', {
        attrs: {
          style: 'top: ' + top + '%;left: ' + left + '%'
        },
        hook: bind('click', e => {
          e.stopPropagation();
          finish(serverRole);
        })
      }, [h('piece.' + serverRole + '.' + color)]);
    }));
  }

  return {
    start,
    cancel,
    view() {
      if (!promoting) return;
      const pieces: Role[] = ['queen', 'knight', 'rook', 'bishop'];
      return renderPromotion(promoting.dest, pieces,
        cgUtil.opposite(getGround().state.turnColor),
        getGround().state.orientation);
    }
  };
}
