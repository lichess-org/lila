import { renderPieceKeys, renderPiecesOn, Style } from './chess';
import { Pieces } from 'shogiground/types';

export const commands = {
  piece: {
    help: 'p: Read locations of a piece type. Example: p N, p k.',
    apply(c: string, pieces: Pieces, style: Style): string | undefined {
      return tryC(c, /^p ([p|n|b|r|q|k|g|s|l|a|m|h|d|u|t])$/i, p => renderPieceKeys(pieces, p, style));
    }
  },
  scan: {
    help: 'scan: Read pieces on a rank or file. Example: scan a, scan 1.',
    apply(c: string, pieces: Pieces, style: Style): string | undefined {
      return tryC(c, /^scan ([a-i1-9])$/i, p => renderPiecesOn(pieces, p, style));
    }
  }
};

function tryC<A>(c: string, regex: RegExp, f: (arg: string) => A | undefined): A | undefined {
  if (!c.match(regex)) return undefined;
  return f(c.replace(regex, '$1'));
}
