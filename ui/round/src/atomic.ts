import * as util from 'shogiground/util';
import * as cg from 'shogiground/types';
import RoundController from './ctrl';

export function capture(ctrl: RoundController, key: cg.Key) {
  const exploding: cg.Key[] = [],
    diff: cg.PiecesDiff = new Map(),
    orig = util.key2pos(key),
    minX = Math.max(0, orig[0] - 1),
    maxX = Math.min(7, orig[0] + 1),
    minY = Math.max(0, orig[1] - 1),
    maxY = Math.min(7, orig[1] + 1);

  for (let x = minX; x <= maxX; x++) {
    for (let y = minY; y <= maxY; y++) {
      const k = util.pos2key([x, y]);
      exploding.push(k);
      const p = ctrl.shogiground.state.pieces.get(k);
      const explodes = p && (k === key || p.role !== 'pawn');
      if (explodes) diff.set(k, undefined);
    }
  }
  ctrl.shogiground.setPieces(diff);
}

// needs to explicitly destroy the capturing pawn
export function enpassant(ctrl: RoundController, key: cg.Key, color: cg.Color) {
  const pos = util.key2pos(key),
    pawnPos: cg.Pos = [pos[0], pos[1] + (color === 'white' ? -1 : 1)];
  capture(ctrl, util.pos2key(pawnPos));
}
