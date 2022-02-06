import { pos2key, invFiles } from './util';
import * as cg from './types';

export const initial: cg.Sfen = 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL';

const roles: { [letter: string]: cg.Role } = {
  p: 'pawn',
  l: 'lance',
  n: 'knight',
  s: 'silver',
  g: 'gold',
  b: 'bishop',
  r: 'rook',
  k: 'king',
  '+p': 'tokin',
  t: 'tokin',
  '+l': 'promotedlance',
  u: 'promotedlance',
  '+n': 'promotedknight',
  m: 'promotedknight',
  '+s': 'promotedsilver',
  a: 'promotedsilver',
  '+b': 'horse',
  h: 'horse',
  '+r': 'dragon',
  d: 'dragon',
};

const letters = {
  pawn: 'p',
  lance: 'l',
  knight: 'n',
  silver: 's',
  gold: 'g',
  bishop: 'b',
  rook: 'r',
  king: 'k',
  tokin: '+p',
  promotedlance: '+l',
  promotedknight: '+n',
  promotedsilver: '+s',
  horse: '+b',
  dragon: '+r',
};

export function getDimensions(sfen: cg.Sfen): cg.Dimensions {
  // todo only if we ever need non square boards...
  const ranks = sfen.split('/').length;
  return { files: ranks, ranks: ranks };
}

export function read(sfen: cg.Sfen, dims: cg.Dimensions): cg.Pieces {
  if (sfen === 'start') sfen = initial;
  const pieces: cg.Pieces = new Map();
  let x = dims.files - 1,
    y = 0;
  for (let i = 0; i < sfen.length; i++) {
    switch (sfen[i]) {
      case ' ':
      case '_':
        return pieces;
      case '/':
        ++y;
        if (y > dims.ranks - 1) return pieces;
        x = dims.files - 1;
        break;
      default:
        const nb = sfen[i].charCodeAt(0);
        if (nb < 58 && nb != 43) x -= nb - 48;
        else {
          const role = sfen[i] === '+' && sfen.length > i + 1 ? '+' + sfen[++i].toLowerCase() : sfen[i].toLowerCase();
          const color = sfen[i] === role || '+' + sfen[i] === role ? 'gote' : 'sente';
          if (x >= 0) {
            pieces.set(pos2key([x, y]), {
              role: roles[role],
              color: color,
            });
          }
          --x;
        }
    }
  }
  return pieces;
}

export function write(pieces: cg.Pieces): cg.Sfen {
  return cg.ranks
    .map(y =>
      invFiles
        .map(x => {
          const piece = pieces.get((x + y) as cg.Key);
          if (piece) {
            const letter = letters[piece.role];
            return piece.color === 'sente' ? letter.toUpperCase() : letter;
          } else return '1';
        })
        .join('')
    )
    .join('/')
    .replace(/1{2,}/g, s => s.length.toString());
}
