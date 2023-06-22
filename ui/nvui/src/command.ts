import { Hands, Pieces } from 'shogiground/types';
import { Style, renderPieceKeys, renderPiecesOn } from './shogi';

export const commands = {
  piece: {
    help: 'p: Read locations of a piece type. Example: p N, p k.',
    apply(c: string, pieces: Pieces, hands: Hands, style: Style): string | undefined {
      return tryC(c, /^p (p|l|n|s|g|b|r|k|\+p|\+l|\+n|\+s|\+b|\+r|d|h|t)$/i, p =>
        renderPieceKeys(pieces, hands, p, style)
      );
    },
  },
  scan: {
    help: 's: Read pieces on a rank or file. Example: s 1, scan a.',
    apply(c: string, pieces: Pieces, style: Style): string | undefined {
      return tryC(c, /^s ([1-9a-i][a-i]?)$/i, p => renderPiecesOn(pieces, p, style));
    },
  },
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  if (!c.match(regex)) return undefined;
  return f(c.replace(regex, '$1'));
}
