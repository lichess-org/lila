import { h } from 'snabbdom'
import { bind, onInsert } from './util';
import * as cgUtil from 'chessground/util';
import { Vm } from './interfaces';

export default function(vm: Vm, getGround, redraw: () => void) {

  let promoting: any = false;

  function start(orig, dest, callback) {
    const g = getGround(),
    piece = g.state.pieces[dest];
    if (piece && piece.role == 'pawn' && (
      (dest[1] == 8 && g.state.turnColor == 'black') ||
        (dest[1] == 1 && g.state.turnColor == 'white'))) {
      promoting = {
        orig: orig,
        dest: dest,
        callback: callback
      };
      redraw();
    return true;
    }
    return false;
  };

  function promote(g, key, role) {
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

  function finish(role) {
    if (promoting) promote(getGround(), promoting.dest, role);
    if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
    promoting = false;
  };

  function cancel() {
    if (promoting) {
      promoting = false;
      getGround().set(vm.cgConfig);
      redraw();
    }
  }

  function renderPromotion(dest, pieces, color, orientation) {
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
  };

  return {

    start,

    cancel,

    view() {
      if (!promoting) return;
      const pieces = ['queen', 'knight', 'rook', 'bishop'];
      return renderPromotion(promoting.dest, pieces,
        cgUtil.opposite(getGround().state.turnColor),
        getGround().state.orientation);
    }
  };
}
