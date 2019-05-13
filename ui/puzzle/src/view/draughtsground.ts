import { h } from 'snabbdom'
import { Draughtsground } from 'draughtsground';
import { Config as CgConfig } from 'draughtsground/config';
import resizeHandle from 'common/resize';

export default function(ctrl) {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Draughtsground((vnode.elm as HTMLElement), makeConfig(ctrl))),
      destroy: _ => ctrl.ground().destroy()
    }
  }, [ h('cg-board') ]);
}

function makeConfig(ctrl): CgConfig {
  const opts = ctrl.makeCgOpts();
  const variant = opts.premovable.variant;
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords,
    addPieceZIndex: ctrl.pref.is3d,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      showDests: ctrl.pref.destination
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
        resizeHandle(elements, ctrl.pref.resizeHandle, ctrl.vm.node.ply);
      }
    },
    premovable: {
      enabled: opts.premovable.enabled,
      variant: variant
    },
    drawable: {
      enabled: true
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight,
      kingMoves: ctrl.pref.showKingMoves && (variant === 'frisian' || variant === 'frysk')
    },
    animation: {
      enabled: true,
      duration: ctrl.pref.animation.duration
    },
    disableContextMenu: true
  };
}
