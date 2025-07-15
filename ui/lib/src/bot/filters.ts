import { Chess, Color, SquareSet, squareFile, squareRank } from 'chessops';

export function pawnStructure(b: Chess): number {
  const color: Color = b.turn;
  const pawns: SquareSet = b.board.pieces(color, 'pawn');

  const fc: Record<number, number> = {};
  const pos: [number, number][] = [];
  let score = 0;

  for (const sq of pawns) {
    const f = squareFile(sq),
      r = squareRank(sq);
    fc[f] = (fc[f] ?? 0) + 1;
    pos.push([f, r]);
    score += (color === 'white' ? r : 7 - r) / 7;
  }

  for (const c of Object.values(fc)) if (c > 1) score -= 0.75 * (c - 1);

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
