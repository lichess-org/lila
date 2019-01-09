import * as util from 'chessground/util';
import * as cg from 'chessground/types';
import RoundController from './ctrl';

export function capture(ctrl: RoundController, key: cg.Key) {
  const exploding: cg.Key[] = [],
  diff: cg.PiecesDiff = {},
  orig = util.key2pos(key),
  minX = Math.max(1, orig[0] - 1),
    maxX = Math.min(8, orig[0] + 1),
    minY = Math.max(1, orig[1] - 1),
    maxY = Math.min(8, orig[1] + 1);
  const pieces = ctrl.chessground.state.pieces;

  for (let x = minX; x <= maxX; x++) {
    for (let y = minY; y <= maxY; y++) {
      const k = util.pos2key([x, y]);
      exploding.push(k);
      const explodes = pieces[k] && (
        k === key || pieces[k]!.role !== 'pawn')
      if (explodes) diff[k] = undefined;
    }
  }
  ctrl.chessground.setPieces(diff);
  ctrl.chessground.explode(exploding);
}

// needs to explicitly destroy the capturing pawn
export function enpassant(ctrl: RoundController, key: cg.Key, color: cg.Color) {
  const pos = util.key2pos(key),
  pawnPos: cg.Pos = [pos[0], pos[1] + (color === 'white' ? -1 : 1)];
  capture(ctrl, util.pos2key(pawnPos));
}
