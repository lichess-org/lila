import { notationFiles, notationRanks } from 'common/notation';
import { predrop, premove } from 'common/pre-sg';
import resizeHandle from 'common/resize';
import { Config as SgConfig } from 'shogiground/config';
import { DrawShape } from 'shogiground/draw';
import * as sg from 'shogiground/types';
import { forsythToRole, roleToForsyth } from 'shogiops/sfen';
import { Piece, Role } from 'shogiops/types';
import { makeSquareName, parseSquareName } from 'shogiops/util';
import {
  handRoles,
  pieceCanPromote,
  pieceForcePromote,
  promotableOnDrop,
  promote as shogiPromote,
} from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from './ctrl';

export function renderBoard(ctrl: AnalyseCtrl): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({
          board: vnode.elm as HTMLElement,
        });
        ctrl.setAutoShapes();
        ctrl.setShapes(ctrl.node.shapes as DrawShape[] | undefined);
      },
    },
  });
}

export function makeConfig(ctrl: AnalyseCtrl): SgConfig {
  const d = ctrl.data,
    variant = d.game.variant.key,
    pref = d.pref,
    opts = ctrl.makeSgOpts();
  const config: SgConfig = {
    turnColor: opts.turnColor,
    activeColor: opts.activeColor,
    sfen: {
      board: opts.sfen?.board,
      hands: opts.sfen?.hands,
    },
    checks: opts.checks,
    lastDests: opts.lastDests,
    orientation: ctrl.bottomColor(),
    viewOnly: !!ctrl.embed,
    hands: {
      roles: handRoles(variant),
      inlined: variant !== 'chushogi',
    },
    movable: {
      free: false,
      dests: opts.movable!.dests,
      showDests: pref.destination,
    },
    droppable: {
      free: false,
      dests: opts.droppable!.dests,
      showDests: pref.dropDestination && pref.destination,
    },
    coordinates: {
      enabled: pref.coords !== 0 && !ctrl.embed,
      files: notationFiles(),
      ranks: notationRanks(),
    },
    events: {
      move: ctrl.userMove,
      drop: ctrl.userDrop,
      unselect: (key: Key) => {
        if (ctrl.lionFirstMove && ctrl.lionFirstMove.to === parseSquareName(key)) {
          const from = ctrl.lionFirstMove.from,
            to = ctrl.lionFirstMove.to;
          ctrl.userMove(makeSquareName(from), makeSquareName(to), false, undefined);
        }
      },
      insert(boardEls?: sg.BoardElements, _handEls?: sg.HandElements) {
        if (!ctrl.embed && boardEls)
          resizeHandle(boardEls, ctrl.data.pref.resizeHandle, { ply: ctrl.node.ply, initialPly: ctrl.mainline[0].ply });
      },
    },
    premovable: {
      enabled: opts.premovable!.enabled,
      showDests: pref.destination,
      generate: opts.premovable!.enabled ? premove(variant) : undefined,
      events: {
        set: ctrl.onPremoveSet,
      },
    },
    predroppable: {
      enabled: opts.predroppable!.enabled,
      generate: opts.predroppable!.enabled ? predrop(variant) : undefined,
      showDests: pref.dropDestination && pref.destination,
    },
    drawable: {
      enabled: !ctrl.embed,
      eraseOnClick: !ctrl.opts.study || !!ctrl.opts.practice,
    },
    promotion: {
      promotesTo: (role: Role) => shogiPromote(variant)(role),
      movePromotionDialog: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece,
          capture = ctrl.shogiground.state.pieces.get(dest) as Piece | undefined;
        return (
          !!piece &&
          pieceCanPromote(variant)(piece, parseSquareName(orig)!, parseSquareName(dest)!, capture) &&
          !pieceForcePromote(variant)(piece, parseSquareName(dest)!)
        );
      },
      dropPromotionDialog(piece) {
        return promotableOnDrop(variant)(piece as Piece);
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece;
        return !!piece && pieceForcePromote(variant)(piece, parseSquareName(dest)!);
      },
    },
    forsyth: {
      fromForsyth: forsythToRole(variant),
      toForsyth: roleToForsyth(variant),
    },
    highlight: {
      lastDests: pref.highlightLastDests,
      check: pref.highlightCheck && variant !== 'chushogi',
    },
    draggable: {
      enabled: pref.moveEvent > 0,
      showGhost: pref.highlightLastDests,
      showTouchSquareOverlay: pref.squareOverlay,
    },
    selectable: {
      enabled: pref.moveEvent !== 1,
    },
    animation: {
      duration: pref.animationDuration,
    },
    disableContextMenu: true,
  };
  ctrl.study && ctrl.study.mutateSgConfig(config);

  return config;
}
