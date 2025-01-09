import { Pieces } from 'shogiground/types';
import { Piece } from 'shogiops/types';
import { SquareSet } from 'shogiops/square-set';
import { makeSquareName } from 'shogiops/util';
import { dimensions, fullSquareSet } from 'shogiops/variant/util';

export function predrop(variant: VariantKey): (piece: Piece, pieces: Pieces) => Key[] {
  return (piece, pieces) => {
    const dims = dimensions(variant),
      limitDrops = variant !== 'kyotoshogi';
    let mask = fullSquareSet(variant);
    if ((piece.role === 'pawn' || piece.role === 'lance') && limitDrops)
      mask = mask.diff(SquareSet.fromRank(piece.color === 'sente' ? 0 : dims.ranks - 1));
    else if (piece.role === 'knight' && limitDrops)
      mask = mask.diff(
        piece.color === 'sente' ? SquareSet.ranksAbove(2) : SquareSet.ranksBelow(dims.ranks - 3),
      );
    return Array.from(mask, s => makeSquareName(s)).filter(k => {
      const p = pieces.get(k);
      return !p || p.color !== piece.color;
    });
  };
}
