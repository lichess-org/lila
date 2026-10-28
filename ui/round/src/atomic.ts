import type { PiecesDiff } from '@lichess-org/chessground/types';
import { key2pos, pos2key } from '@lichess-org/chessground/util';

import type RoundController from './ctrl';

export function capture(ctrl: RoundController, key: Key): void {
  const exploding: Key[] = [];
  const diff: PiecesDiff = new Map();
  const orig = key2pos(key);
  const minX = Math.max(0, orig[0] - 1);
  const maxX = Math.min(7, orig[0] + 1);
  const minY = Math.max(0, orig[1] - 1);
  const maxY = Math.min(7, orig[1] + 1);

  for (let x = minX; x <= maxX; x++) {
    for (let y = minY; y <= maxY; y++) {
      const k = pos2key([x, y]);
      if (k) {
        exploding.push(k);
        const p = ctrl.chessground.state.pieces.get(k);
        const explodes = p && (k === key || p.role !== 'pawn');
        if (explodes) diff.set(k, undefined);
      }
    }
  }
  ctrl.chessground.setPieces(diff);
  ctrl.chessground.explode(exploding);
}
