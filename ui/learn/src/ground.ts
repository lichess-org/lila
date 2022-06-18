import { Config } from 'shogiground/config';
import { Dests, DropDests } from 'shogiground/types';
import LearnCtrl from './ctrl';
import { shogigroundDropDests, shogigroundDests } from 'shogiops/compat';
import { pieceCanPromote, pieceInDeadZone, promote } from 'shogiops/variantUtil';
import { Role, isDrop } from 'shogiops/types';
import { makeSquare, parseSquare, parseUsi } from 'shogiops/util';
import { Level, UsiWithColor } from './interfaces';
import { currentPosition } from './util';
import { Drawable, SquareHighlight } from 'shogiground/draw';
import { illegalShogigroundDests, illegalShogigroundDropDests, inCheck } from './shogi';

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
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return (
          !!piece &&
          pieceCanPromote('standard')(piece, parseSquare(orig)!, parseSquare(dest)!) &&
          !pieceInDeadZone('standard')(piece, parseSquare(dest)!)
        );
      },
      forceMovePromotion: (orig: Key, dest: Key) => {
        const piece = ctrl.shogiground.state.pieces.get(orig);
        return !!piece && pieceInDeadZone('standard')(piece, parseSquare(dest)!);
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
      notation: ctrl.pref.notation,
    },
    drawable: {
      enabled: true,
      visible: true,
    },
    events: {
      move: ctrl.onMove.bind(ctrl),
      drop: ctrl.onDrop.bind(ctrl),
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlightLastDests,
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1,
    },
    highlight: {
      check: ctrl.pref.highlightCheck,
      lastDests: ctrl.pref.highlightLastDests,
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
    check: !hasObstacles && inCheck(pos),
    movable: {
      dests: (illegalDests ? illegalShogigroundDests(pos) : shogigroundDests(pos)) as Dests,
    },
    droppable: {
      dests: (illegalDests ? illegalShogigroundDropDests(pos) : shogigroundDropDests(pos)) as DropDests,
    },
  };
}

export function createDrawable(level: Level, usiCList: UsiWithColor[] = []): Partial<Drawable> {
  const obastacles: SquareHighlight[] = [],
    usis = usiCList.map(uc => uc.usi),
    dests = usis.map(u => makeSquare(parseUsi(u)!.to)),
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
    ctrl.shogiground.drop({ role: moveOrDrop.role, color: usiC.color }, makeSquare(moveOrDrop.to) as Key);
  } else {
    ctrl.shogiground.move(makeSquare(moveOrDrop.from) as Key, makeSquare(moveOrDrop.to) as Key, moveOrDrop.promotion);
  }
}
