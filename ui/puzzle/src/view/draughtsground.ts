import { h } from 'snabbdom'
import { Draughtsground } from 'draughtsground';
import { Config as CgConfig } from 'draughtsground/config';

export default function(ctrl) {
  return h('div.cg-board-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Draughtsground((vnode.elm as HTMLElement), makeConfig(ctrl))),
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
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords,
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
      move: ctrl.userMove
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
