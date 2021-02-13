import { renderPieceKeys, renderPiecesOn, Style } from './chess';
import { Pieces } from 'chessground/types';

export const commands = {
  piece: {
    help: 'p: Read locations of a piece type. Example: p N, p k.',
    apply(c: string, pieces: Pieces, style: Style): string | undefined {
      return tryC(c, /^p ([p|n|b|r|q|k])$/i, p => renderPieceKeys(pieces, p, style));
    },
  },
  scan: {
    help: 's: Read pieces on a rank or file. Example: s a, s 1.',
    apply(c: string, pieces: Pieces, style: Style): string | undefined {
      return tryC(c, /^s ([a-h1-8])$/i, p => renderPiecesOn(pieces, p, style));
    },
  },
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  if (!c.match(regex)) return undefined;
  return f(c.replace(regex, '$1'));
}
