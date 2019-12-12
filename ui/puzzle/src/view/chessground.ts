import { h } from 'snabbdom'
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import resizeHandle from 'common/resize';

export default function(ctrl) {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground((vnode.elm as HTMLElement), makeConfig(ctrl))),
      destroy: _ => ctrl.ground().destroy()
    }
  });
}

function makeConfig(ctrl): CgConfig {
  const opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== 0,
    addPieceZIndex: ctrl.pref.is3d,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      showDests: ctrl.pref.destination,
      rookCastle: ctrl.pref.rookCastle
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlight
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1
    },
    events: {
      move: ctrl.userMove,
      insert(elements) {
        resizeHandle(
          elements,
          ctrl.pref.resizeHandle,
          ctrl.vm.node.ply,
          (_) => true
        )
      }
    },
    premovable: {
      enabled: opts.premovable.enabled
    },
    drawable: {
      enabled: true
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration
    },
    disableContextMenu: true
  };
}
