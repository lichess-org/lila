import { Pieces } from 'shogiground/types';
import { attacks } from 'shogiops/attacks';
import { Piece } from 'shogiops/types';
import { SquareSet } from 'shogiops/square-set';
import { makeSquareName, parseSquareName } from 'shogiops/util';
import { directlyBehind } from 'shogiops/variant/annanshogi';
import { fullSquareSet } from 'shogiops/variant/util';

export function premove(variant: VariantKey): (key: Key, pieces: Pieces) => Key[] {
  return (key, pieces) => {
    const piece = pieces.get(key) as Piece | undefined;
    if (piece) {
      const attackingPiece: Piece =
        variant === 'annanshogi'
          ? ((pieces.get(makeSquareName(directlyBehind(piece.color, parseSquareName(key)))) ??
              piece) as Piece)
          : piece;
      return Array.from(
        attacks(attackingPiece, parseSquareName(key), SquareSet.empty()).intersect(
          fullSquareSet(variant),
        ),
        s => makeSquareName(s),
      );
    } else return [];
  };
}
