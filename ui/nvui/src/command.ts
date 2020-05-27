import { renderPieceKeys, renderPiecesOn } from './draughts';
import { BoardSize, Pieces } from 'draughtsground/types';

export const commands = {
  piece: {
    help: '/p: Read locations of a piece type. Example: /p M, /p k.',
    apply(c: string, pieces: Pieces): string | undefined {
      return tryC(c, /^p ([m|k])$/i, p => renderPieceKeys(pieces, p));
    }
  },
  scan: {
    help: '/scan:Read pieces on a horizontal line. Example: /scan 4, /scan 10.',
    apply(c: string, pieces: Pieces, boardSize: BoardSize, reverse?: boolean): string | undefined {
      return tryC(c, /^scan ([1-9]|10)$/i, p => renderPiecesOn(pieces, boardSize, parseInt(p), reverse));
    }
  }
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  if (!c.match(regex)) return undefined;
  return f(c.replace(regex, '$1'));
}
