import { Pieces } from 'shogiground/types';
import { SquareSet, makeSquare, parseSquare } from 'shogiops';
import { attacks } from 'shogiops/attacks';
import { Piece } from 'shogiops/types';
import { dimensions, fullSquareSet } from 'shogiops/variant/util';

export function premove(variant: VariantKey): (key: Key, pieces: Pieces) => Key[] {
  return (key, pieces) => {
    const piece = pieces.get(key) as Piece | undefined;
    if (piece)
      return Array.from(attacks(piece, parseSquare(key), SquareSet.empty()).intersect(fullSquareSet(variant)), s =>
        makeSquare(s)
      );
    else return [];
  };
}

export function predrop(variant: VariantKey): (piece: Piece, pieces: Pieces) => Key[] {
  return (piece, pieces) => {
    const dims = dimensions(variant);
    let mask = fullSquareSet(variant);
    if (piece.role === 'pawn' || piece.role === 'lance')
      mask = mask.diff(SquareSet.fromRank(piece.color === 'sente' ? 0 : dims.ranks - 1));
    else if (piece.role === 'knight')
      mask = mask.diff(piece.color === 'sente' ? SquareSet.ranksAbove(2) : SquareSet.ranksBelow(dims.ranks - 3));
    return Array.from(mask, s => makeSquare(s)).filter(k => {
      const p = pieces.get(k);
      return !p || p.color !== piece.color;
    });
  };
}
