import { squareFile, squareRank } from 'shogiops';
import { Move, Square, isDrop } from 'shogiops/types';

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'sente' : 'gote';
}

export function scalashogiCharPair(move: Move): string {
  const charOffset = 35;
  function squareToCharCode(sq: Square): number {
    return charOffset + squareRank(sq) * 9 + squareFile(sq);
  }
  if (isDrop(move))
    return String.fromCharCode(
      squareToCharCode(move.to),
      charOffset + 81 + ['rook', 'bishop', 'gold', 'silver', 'knight', 'lance', 'pawn'].indexOf(move.role)
    );
  else {
    const from = squareToCharCode(move.from),
      to = squareToCharCode(move.to);
    if (move.promotion) return String.fromCharCode(to, from);
    else return String.fromCharCode(from, to);
  }
}
