import resizeHandle from 'common/resize';
import { Config as SgConfig } from 'shogiground/config';
//mport { Role } from 'shogiops/types';
//mport { parseSquareName } from 'shogiops/util';
//mport { pieceCanPromote, pieceInDeadZone, promote } from 'shogiops/variant/util';
import { PuzPrefs, UserDrop, UserMove } from '../interfaces';

export function makeConfig(opts: SgConfig, pref: PuzPrefs, userMove: UserMove, userDrop: UserDrop): SgConfig {
  return {
    sfen: opts.sfen,
    activeColor: opts.activeColor,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    checks: opts.checks,
    lastDests: opts.lastDests,
    coordinates: { enabled: pref.coords !== 0 },
    movable: {
      free: false,
      dests: opts.movable!.dests,
      showDests: pref.destination,
    },
    droppable: {
      free: false,
      dests: opts.droppable!.dests,
      showDests: pref.destination && pref.dropDestination,
    },
    //promotion: {
    //  promotesTo: (role: Role) => {
    //    return promote('standard')(role);
    //  },
    //  movePromotionDialog: (orig: Key, dest: Key) => {
    //    const piece = ctrl.shogiground.state.pieces.get(orig);
    //    return (
    //      !!piece &&
    //      pieceCanPromote(ctrl.data.game.variant.key)(piece, parseSquareName(orig)!, parseSquareName(dest)!) &&
    //      !pieceInDeadZone(ctrl.data.game.variant.key)(piece, parseSquareName(dest)!)
    //    );
    //  },
    //  forceMovePromotion: (orig: Key, dest: Key) => {
    //    const piece = ctrl.shogiground.state.pieces.get(orig);
    //    return !!piece && pieceInDeadZone(ctrl.data.game.variant.key)(piece, parseSquareName(dest)!);
    //  },
    //},
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlightLastDests,
      showTouchSquareOverlay: pref.squareOverlay,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    events: {
      move: userMove,
      drop: userDrop,
      insert(elements) {
        if (elements) resizeHandle(elements, pref.resizeHandler, { visible: () => true });
      },
    },
    premovable: {
      enabled: false,
    },
    predroppable: {
      enabled: false,
    },
    drawable: {
      enabled: true,
    },
    highlight: {
      lastDests: pref.highlightLastDests,
      check: pref.highlightCheck,
    },
    animation: {
      enabled: false,
    },
  };
}
