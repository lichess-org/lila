import { Pieces } from 'shogiground/types';
import { SquareSet, makeSquareName, parseSquareName } from 'shogiops';
import { attacks } from 'shogiops/attacks';
import { Piece } from 'shogiops/types';
import { directlyBehind } from 'shogiops/variant/annanshogi';
import { dimensions, fullSquareSet } from 'shogiops/variant/util';

export function premove(variant: VariantKey): (key: Key, pieces: Pieces) => Key[] {
  return (key, pieces) => {
    const piece = pieces.get(key) as Piece | undefined;
    if (piece) {
      const attackingPiece: Piece =
        variant === 'annanshogi'
          ? ((pieces.get(makeSquareName(directlyBehind(piece.color, parseSquareName(key)))) ?? piece) as Piece)
          : piece;
      return Array.from(
        attacks(attackingPiece, parseSquareName(key), SquareSet.empty()).intersect(fullSquareSet(variant)),
        s => makeSquareName(s)
      );
    } else return [];
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
    return Array.from(mask, s => makeSquareName(s)).filter(k => {
      const p = pieces.get(k);
      return !p || p.color !== piece.color;
    });
  };
}
