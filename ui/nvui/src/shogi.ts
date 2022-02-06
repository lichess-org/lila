import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { Pieces, files, ranks } from 'shogiground/types';
import { invFiles, allKeys } from 'shogiground/util';
import { Setting, makeSetting } from './setting';

export type Style = 'usi' | 'literate' | 'nato' | 'anna';

const nato: { [letter: string]: string } = {
  a: 'alpha',
  b: 'bravo',
  c: 'charlie',
  d: 'delta',
  e: 'echo',
  f: 'foxtrot',
  g: 'golf',
  h: 'hotel',
  i: 'india',
};
const anna: { [letter: string]: string } = {
  a: 'anna',
  b: 'bella',
  c: 'cesar',
  d: 'david',
  e: 'eva',
  f: 'felix',
  g: 'gustav',
  h: 'hector',
  i: 'ivan',
};
const roles: { [letter: string]: string } = {
  P: 'pawn',
  R: 'rook',
  N: 'knight',
  B: 'bishop',
  K: 'king',
  G: 'gold',
  S: 'silver',
  L: 'lance',
  '+P': 'tokin',
  '+S': 'promoted silver',
  '+N': 'promoted knight',
  '+L': 'promoted lance',
  '+B': 'horse',
  '+R': 'dragon',
};
const letters = {
  pawn: 'p',
  rook: 'r',
  knight: 'n',
  bishop: 'b',
  king: 'k',
  gold: 'g',
  silver: 's',
  lance: 'l',
  tokin: '+p',
  promotedsilver: '+s',
  promotedknight: '+n',
  promotedlance: '+l',
  horse: '+b',
  dragon: '+r',
};

export function supportedVariant(key: string) {
  return ['standard'].includes(key);
}

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['usi', 'USI: 7g7f'],
      ['literate', 'Literate: knight takes 3 f'],
      ['anna', 'Anna: knight takes 3 felix'],
      ['nato', 'Nato: knight takes 3 foxtrot'],
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: window.lishogi.storage.make('nvui.moveNotation'),
  });
}

export function renderMove(usi: Usi | undefined, style: Style) {
  if (!usi) return '';
  let move: string;
  if (style === 'usi') move = usi;
  else {
    // todo - western/japanese notation
    move = usi;
  }
  return move;
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h(
    'div',
    ['sente', 'gote'].map(color => {
      const lists: any = [];
      [
        'king',
        'rook',
        'bishop',
        'knight',
        'pawn',
        'gold',
        'silver',
        'lance',
        'tokin',
        'promotedsilver',
        'promotedknight',
        'promotedlance',
        'horse',
        'dragon',
      ].forEach(role => {
        const keys = [];
        for (const [key, piece] of pieces) {
          if (piece.color === color && piece.role === role) keys.push(key);
        }
        if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
      });
      return h('div', [
        h('h3', `${color} pieces`),
        ...lists
          .map(
            (l: any) =>
              `${l[0]}: ${l
                .slice(1)
                .map((k: string) => renderKey(k, style))
                .join(', ')}`
          )
          .join(', '),
      ]);
    })
  );
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const name = `${p === p.toUpperCase() ? 'sente' : 'gote'} ${roles[p.toUpperCase()]}`;
  const res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k as Key);
  }
  return `${name}: ${res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFile)) {
      const piece = pieces.get(k);
      if (piece) res.push(`${renderKey(k, style)} ${piece.color} ${piece.role}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
}

export function renderBoard(pieces: Pieces, pov: Color): string {
  const board = [[' ', ...files, ' ']];
  for (let rank of ranks) {
    let line = [];
    for (let file of invFiles) {
      let key = (file + rank) as Key;
      const piece = pieces.get(key);
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'sente' ? letter.toUpperCase() : letter);
      } else line.push((key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...files, ' ']);
  if (pov === 'gote') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}

export function renderFile(f: string, style: Style): string {
  return style === 'nato' ? nato[f] : style === 'anna' ? anna[f] : f;
}

export function renderKey(key: string, style: Style): string {
  return `${renderFile(key[0], style)} ${key[1]}`;
}
