import { Pieces, files, ranks } from 'shogiground/types';
import { allKeys } from 'shogiground/util';
import { parseSfen, pieceToForsyth } from 'shogiops/sfen';
import { Piece, Role, isDrop } from 'shogiops/types';
import { VNode, h } from 'snabbdom';
import { Setting, makeSetting } from './setting';
import { dimensions } from 'shogiops/variant/util';
import { parseUsi } from 'shogiops';
import { toKanjiDigit } from 'shogiops/notation/util';

export type Style = 'usi' | 'literate' | 'nato' | 'anna' | 'japanese';

const supportedVariants: VariantKey[] = ['standard', 'minishogi', 'annanshogi', 'kyotoshogi'];

export function supportedVariant(key: VariantKey) {
  return supportedVariants.includes(key);
}

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['usi', 'USI: pawn, 7g, 7f'],
      ['literate', 'Literate: pawn, 7 g 7 f'],
      ['anna', 'Anna: pawn, 7 gustav 3 felix'],
      ['nato', 'Nato: pawn, 7 golf 3 foxtrot'],
      ['japanese', 'Japanese: ７ 七 ３ 六'],
    ],
    default: useJP() ? 'japanese' : 'anna',
    storage: window.lishogi.storage.make('nvui.moveNotation'),
  });
}

// sfen of node, therefore after usi was performed
export function renderMove(usi: Usi | undefined, sfen: Sfen, variant: VariantKey, style: Style): string {
  if (!usi) return '';
  const pos = parseSfen(variant, sfen, false),
    move = parseUsi(usi);
  if (pos.isErr || !move) return '';
  const piece = pos.value.board.get(move.to)!;
  if (isDrop(move)) {
    const to = usi.slice(2, 4) as Key;
    return renderRole(piece.role) + ' * ' + renderKey(to, style);
  } else {
    const from = usi.slice(0, 2) as Key,
      to = usi.slice(2, 4) as Key;
    return [renderRole(piece.role), renderKey(from, style), renderKey(to, style)].join(' ');
  }
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h(
    'div',
    ['sente', 'gote'].map((color: 'sente' | 'gote') => {
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
        let name;
        if (useJP()) name = rolesJP[role];
        else name = `${role}${keys.length > 1 ? 's' : ''}`;
        if (keys.length) lists.push([name, ...keys]);
      });
      return h('div', [
        h('h3', `${useJP() ? colorsJP[color] : color} pieces:`),
        ...lists
          .map(
            (l: any) =>
              `${l[0]}: ${l
                .slice(1)
                .map((k: Key) => renderKey(k, style))
                .join(', ')}`
          )
          .join(', '),
      ]);
    })
  );
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const role = letterToRole(p);
  if (!role) return 'Invalid piece';
  const name = `${p === p.toUpperCase() ? 'sente' : 'gote'} ${role}`,
    res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k);
  }
  return `${useJP() ? `${p === p.toUpperCase() ? '先手' : '後手'} ${rolesJP[role]}` : name}: ${
    res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'
  }`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFileOrBoth: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFileOrBoth)) {
      const piece = pieces.get(k) as Piece | undefined;
      if (piece) res.push(`${renderKey(k, style)} ${renderPiece(piece.color, piece.role)}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
}

export function renderBoard(pieces: Pieces, pov: Color, variant: VariantKey): string {
  const reversedFiles = [...files].slice(0, dimensions(variant).files).reverse(),
    board = [[' ', ...reversedFiles, ' ']];
  for (let rank of ranks.slice(0, dimensions(variant).ranks)) {
    let line = [];
    for (let file of reversedFiles) {
      let key = (file + rank) as Key;
      const piece = pieces.get(key) as Piece;
      if (piece) {
        const letter = pieceToForsyth(variant)(piece);
        line.push(letter);
      } else line.push('-');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...reversedFiles, ' ']);
  if (pov === 'gote') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}

export function validUsi(usi: Usi, sfen: Sfen, variant: VariantKey): Usi | undefined {
  const pos = parseSfen(variant, sfen, false),
    move = parseUsi(usi);
  if (pos.isOk && move) return pos.value.isLegal(move) ? usi : undefined;
  return undefined;
}

function letterToRole(l: string): Role | undefined {
  switch (l.toUpperCase()) {
    case 'P':
      return 'pawn';
    case 'R':
      return 'rook';
    case 'B':
      return 'bishop';
    case 'L':
      return 'lance';
    case 'N':
      return 'knight';
    case 'S':
      return 'silver';
    case 'G':
      return 'gold';
    case 'K':
      return 'king';
    case '+P':
    case 'T':
      return 'tokin';
    case '+R':
    case 'D':
      return 'dragon';
    case '+B':
    case 'H':
      return 'horse';
    case '+L':
      return 'promotedlance';
    case '+N':
      return 'promotedknight';
    case '+S':
      return 'promotedsilver';
    default:
      return undefined;
  }
}

const rolesJP: { [letter: string]: string } = {
  pawn: '歩',
  rook: '飛',
  knight: '桂',
  bishop: '角',
  king: '王',
  gold: '金',
  silver: '銀',
  lance: '香',
  tokin: 'と',
  promotedsilver: '成銀',
  'promoted silver': '成銀',
  promotedknight: '成桂',
  'promoted knight': '成桂',
  promotedlance: '成香',
  'promoted lance': '成香',
  horse: '馬',
  dragon: '龍',
};

const colorsJP = {
  sente: '先手',
  gote: '後手',
};

export function renderRole(r: Role): string {
  if (useJP()) return rolesJP[r];
  else return r.replace(/^promoted/, 'promoted ');
}

export function renderPiece(c: Color, r: Role): string {
  return `${useJP() ? colorsJP[c] : c} ${renderRole(r)}`;
}

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

export function renderFile(f: string, style: Style): string {
  if (style === 'japanese') return String.fromCharCode(f.charCodeAt(0) + 0xfee0);
  else return f;
}

export function renderRank(f: string, style: Style): string {
  if (style === 'japanese') return toKanjiDigit((f.toLowerCase().charCodeAt(0) - 96).toString());
  else if (style === 'nato') return nato[f];
  else if (style === 'anna') return anna[f];
  else return f;
}

export function renderKey(key: Key, style: Style): string {
  if (style === 'usi') return key;
  else return `${renderFile(key[0], style)} ${renderRank(key[1], style)} `;
}

function useJP(): boolean {
  return document.documentElement.lang === 'ja-JP';
}
