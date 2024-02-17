import { notationFiles, notationRanks } from 'common/notation';
import resizeHandle from 'common/resize';
import { Config } from 'shogiground/config';
import { Drawable, SquareHighlight } from 'shogiground/draw';
import { shogigroundDropDests, shogigroundMoveDests } from 'shogiops/compat';
import { Piece, Role, isDrop } from 'shogiops/types';
import { makeSquareName, parseSquareName, parseUsi } from 'shogiops/util';
import { pieceCanPromote, pieceForcePromote, promote } from 'shogiops/variant/util';
import LearnCtrl from './ctrl';
import { Level, UsiWithColor } from './interfaces';
import { illegalShogigroundDropDests, illegalShogigroundMoveDests, inCheck } from './shogi';
import { currentPosition } from './util';

export function initConfig(ctrl: LearnCtrl): Config {
  return {
    movable: {
      free: false,
      events: {
        after: ctrl.onUserMove.bind(ctrl),
      },
    },
    droppable: {
      free: false,
      events: {
        after: ctrl.onUserDrop.bind(ctrl),
      },
    },
    promotion: {
      promotesTo: (role: Role) => {
        return promote('standard')(role);
      },
      movePromotionDialog: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece;
        return (
          !!piece &&
          pieceCanPromote('standard')(piece, parseSquareName(orig)!, parseSquareName(dest)!, undefined) &&
          !pieceForcePromote('standard')(piece, parseSquareName(dest)!)
        );
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig) as Piece;
        return !!piece && pieceForcePromote('standard')(piece, parseSquareName(dest)!);
      },
    },
    predroppable: {
      enabled: false,
    },
    premovable: {
      enabled: false,
    },
    coordinates: {
      enabled: true,
      files: notationFiles(),
      ranks: notationRanks(),
    },
    drawable: {
      enabled: true,
      visible: true,
    },
    events: {
      move: ctrl.onMove.bind(ctrl),
      drop: ctrl.onDrop.bind(ctrl),
      insert(boardEls) {
        if (boardEls) resizeHandle(boardEls, ctrl.pref.resizeHandle, { visible: () => true });
      },
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlightLastDests,
      showTouchSquareOverlay: ctrl.pref.squareOverlay,
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1,
    },
    highlight: {
      check: ctrl.pref.highlightCheck,
      lastDests: ctrl.pref.highlightLastDests,
    },
    hands: {
      inlined: true,
    },
  };
}

export function initLevelConfig(level: Level): Config {
  const splitSfen = level.sfen.split(' ');
  return {
    sfen: {
      board: splitSfen[0],
      hands: splitSfen[2],
    },
    turnColor: level.color,
    activeColor: level.color,
    orientation: level.color,
    lastDests: [],
    drawable: createDrawable(level),
  };
}

export function destsAndCheck(level: Level, usiCList: UsiWithColor[]): Config {
  const pos = currentPosition(level, usiCList),
    hasObstacles = !!level.obstacles?.length,
    illegalDests = !!level.offerIllegalDests || hasObstacles;

  return {
    checks: !hasObstacles && inCheck(pos),
    movable: {
      dests: illegalDests ? illegalShogigroundMoveDests(pos) : shogigroundMoveDests(pos),
    },
    droppable: {
      dests: illegalDests ? illegalShogigroundDropDests(pos) : shogigroundDropDests(pos),
    },
  };
}

export function createDrawable(level: Level, usiCList: UsiWithColor[] = []): Partial<Drawable> {
  const obastacles: SquareHighlight[] = [],
    usis = usiCList.map(uc => uc.usi),
    dests = usis.map(u => makeSquareName(parseUsi(u)!.to)),
    drawShapes = level.drawShapes && level.drawShapes(level, usiCList),
    squares = level.squareHighlights && level.squareHighlights(level, usiCList);

  if (level.obstacles)
    level.obstacles.map(o => {
      if (!dests.includes(o)) obastacles.push({ key: o, className: 'star' });
    });

  return {
    shapes: drawShapes || [],
    squares: (squares || []).concat(obastacles),
  };
}

export function playUsi(ctrl: LearnCtrl, usiC: UsiWithColor): void {
  const moveOrDrop = parseUsi(usiC.usi)!;
  if (isDrop(moveOrDrop)) {
    ctrl.shogiground.drop({ role: moveOrDrop.role, color: usiC.color }, makeSquareName(moveOrDrop.to));
  } else {
    ctrl.shogiground.move(makeSquareName(moveOrDrop.from), makeSquareName(moveOrDrop.to), moveOrDrop.promotion);
  }
}
