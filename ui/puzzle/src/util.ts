import { squareFile, squareRank } from 'shogiops/util';
import { MoveOrDrop, Square, isDrop } from 'shogiops/types';

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'sente' : 'gote';
}

export function scalashogiCharPair(md: MoveOrDrop): string {
  const charOffset = 35;
  function squareToCharCode(sq: Square): number {
    return charOffset + squareRank(sq) * 9 + squareFile(sq);
  }
  if (isDrop(md))
    return String.fromCharCode(
      squareToCharCode(md.to),
      charOffset + 81 + ['rook', 'bishop', 'gold', 'silver', 'knight', 'lance', 'pawn'].indexOf(md.role)
    );
  else {
    const from = squareToCharCode(md.from),
      to = squareToCharCode(md.to);
    if (md.promotion) return String.fromCharCode(to, from);
    else return String.fromCharCode(from, to);
  }
}
