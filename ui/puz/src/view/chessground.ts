import resizeHandle from 'common/resize';
import type { PuzPrefs, UserMove } from '../interfaces';
import { ShowResizeHandle, Coords } from 'common/prefs';
import { storage } from 'common/storage';

export function makeConfig(opts: CgConfig, pref: PuzPrefs, userMove: UserMove): CgConfig {
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: pref.coords !== Coords.Hidden,
    coordinatesOnSquares: pref.coords === Coords.All,
    addPieceZIndex: pref.is3d,
    addDimensionsCssVarsTo: document.body,
    movable: {
      free: false,
      color: opts.movable!.color,
      dests: opts.movable!.dests,
      showDests: pref.destination,
      rookCastle: pref.rookCastle,
    },
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlight,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    events: {
      move: userMove,
      insert(elements) {
        resizeHandle(elements, ShowResizeHandle.OnlyAtStart, 0, p => p === 0);
      },
    },
    premovable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
      defaultSnapToValidMove: storage.boolean('arrow.snap').getOrDefault(true),
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
