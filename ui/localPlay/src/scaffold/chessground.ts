import resizeHandle from 'common/resize';
import { Config as CgConfig } from 'chessground/config';
import * as Prefs from 'common/prefs';
import { CgHost } from './interfaces';

const pref = {
  coords: Prefs.Coords.Hidden,
  is3d: false,
  destination: false,
  rookCastle: false,
  moveEvent: 0,
  highlight: false,
  animation: 0,
};

export function makeConfig(ctrl: CgHost): CgConfig {
  const opts = ctrl.cgOpts(true);
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: true, //pref.coords !== Prefs.Coords.Hidden,
    addPieceZIndex: pref.is3d,
    addDimensionsCssVarsTo: document.body,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: true, //pref.destination,
      rookCastle: pref.rookCastle,
      events: {
        after: ctrl.cgUserMove,
      },
    },
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlight,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    events: {
      insert(elements) {
        resizeHandle(elements, Prefs.ShowResizeHandle.OnlyAtStart, 0, p => p == 0);
      },
    },
    premovable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: site.storage.boolean('arrow.snap').getOrDefault(true),
    },
    highlight: {
      lastMove: pref.highlight,
      check: pref.highlight,
    },
    animation: {
      duration: pref.animation,
    },
    disableContextMenu: true,
  };
}
