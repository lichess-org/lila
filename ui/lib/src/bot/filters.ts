import * as co from 'chessops';

// michael's python algorithm: https://hq.lichess.ovh/#narrow/channel/8-dev/topic/Fancy.20Bots/near/3803327

export function pawnStructure(b: co.Chess): number {
  const color: Color = co.opposite(b.turn);
  const pawns: co.SquareSet = b.board.pieces(color, 'pawn');
  const fc: Record<number, number> = {};
  const pos: [number, number][] = [];
  let score = 0;

  // Advancement score
  for (const sq of pawns) {
    const f = co.squareFile(sq),
      r = co.squareRank(sq);
    fc[f] = (fc[f] ?? 0) + 1;
    pos.push([f, r]);
    score += (color === 'white' ? r : 7 - r) / 7.0;
  }
  // Penalize doubled pawns
  for (const c of Object.values(fc)) if (c > 1) score -= 0.75 * (c - 1);

  // Reward tightly connected pawns (within 1 square including diagonally)
  let conn = 0;
  for (let i = 0; i < pos.length; i++) {
    const [fx, fy] = pos[i];
    for (let j = i + 1; j < pos.length; j++) {
      const [sx, sy] = pos[j];
      if (Math.abs(fx - sx) === 1 && Math.abs(fy - sy) <= 1) conn++;
    }
  }

  score += 0.5 * conn;

  return Math.max(0, Math.min(1, (score + 2) / 12.5));
}
