import { Hand, Hands, Pieces, files, ranks } from 'shogiground/types';
import { allKeys, opposite } from 'shogiground/util';
import { boardToSfen } from 'shogiground/sfen';
import { parseBoardSfen, parseSfen, pieceToForsyth, roleToForsyth } from 'shogiops/sfen';
import { Piece, Role, isDrop } from 'shogiops/types';
import { VNode, h } from 'snabbdom';
import { Setting, makeSetting } from './setting';
import { dimensions, handRoles } from 'shogiops/variant/util';
import { makeUsi, parseUsi } from 'shogiops/util';
import { toKanjiDigit } from 'shogiops/notation/util';
import { makeKifBoard } from 'shogiops/notation/kif/kif';
import { toMoveOrDrop } from 'keyboardMove/dist/plugins/util'; // todo better later

export type Style = 'usi' | 'literate' | 'nato' | 'anna' | 'japanese';

const supportedVariants: VariantKey[] = ['standard', 'minishogi', 'annanshogi', 'kyotoshogi', 'checkshogi'];

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
    default: document.documentElement.lang === 'ja-JP' ? 'japanese' : 'anna',
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
    return renderRole(piece.role, style) + ' * ' + renderKey(to, style);
  } else {
    const from = usi.slice(0, 2) as Key,
      to = usi.slice(2, 4) as Key;
    return [renderRole(piece.role, style), renderKey(from, style), renderKey(to, style)].join(' ');
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
        if (style === 'japanese') name = rolesJP[role];
        else name = `${role}${keys.length > 1 ? 's' : ''}`;
        if (keys.length) lists.push([name, ...keys]);
      });
      return h('div', [
        h('h3', `${renderColor(color, style)} pieces:`),
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

export function renderPieceKeys(pieces: Pieces, hands: Hands, p: string, style: Style): string {
  const role = letterToRole(p);
  if (!role) return 'Invalid piece';
  const color = p === p.toUpperCase() ? 'sente' : 'gote',
    name = `${color} ${role}`,
    res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k);
  }
  const handStr = (hands.get(color)?.get(role) || 'none') + ' in hand';
  return `${style === 'japanese' ? `${renderColor(color, style)} ${rolesJP[role]}` : name}: ${
    res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'
  }; ${handStr}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFileOrBoth: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFileOrBoth)) {
      const piece = pieces.get(k) as Piece | undefined;
      if (piece) res.push(`${renderKey(k, style)} ${renderPiece(piece.color, piece.role, style)}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
}

export function renderHand(
  position: 'top' | 'bottom',
  pov: Color,
  hand: Hand | undefined,
  variant: VariantKey,
  style: Style
): string {
  let handStr = '',
    color = position === 'top' ? opposite(pov) : pov;
  if (style === 'japanese') color = position === 'top' ? 'gote' : 'sente';
  if (!hand) return '-';
  for (const role of handRoles(variant)) {
    const forsyth = pieceToForsyth(variant)({ color, role });
    if (forsyth) {
      const senteCnt = hand.get(role);
      if (senteCnt) handStr += senteCnt > 1 ? senteCnt.toString() + forsyth : forsyth;
    }
  }
  return renderColor(color, style) + ': ' + (handStr || '-');
}

export function renderBoard(pieces: Pieces, pov: Color, variant: VariantKey, style: Style): string {
  if (style === 'japanese') {
    const boardSfen = boardToSfen(pieces, dimensions(variant), roleToForsyth(variant)),
      board = parseBoardSfen(variant, boardSfen);
    if (board.isOk) return makeKifBoard(variant, board.value);
  }
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

export function validUsi(moveString: string, sfen: Sfen, variant: VariantKey): Usi | undefined {
  const pos = parseSfen(variant, sfen, false);
  if (pos.isOk) {
    const move = toMoveOrDrop(moveString.toLowerCase(), pos.value);
    if (move) return makeUsi(move);
  }
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

function renderColor(c: Color, s: Style): string {
  if (s === 'japanese') return colorsJP[c];
  else return c;
}

function renderRole(r: Role, s: Style): string {
  if (s === 'japanese') return rolesJP[r];
  else return r.replace(/^promoted/, 'promoted ');
}

function renderPiece(c: Color, r: Role, s: Style): string {
  return `${renderColor(c, s)} ${renderRole(r, s)}`;
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

function renderFile(f: string, style: Style): string {
  if (style === 'japanese') return String.fromCharCode(f.charCodeAt(0) + 0xfee0);
  else return f;
}

function renderRank(f: string, style: Style): string {
  if (style === 'japanese') return toKanjiDigit((f.toLowerCase().charCodeAt(0) - 96).toString());
  else if (style === 'nato') return nato[f];
  else if (style === 'anna') return anna[f];
  else return f;
}

function renderKey(key: Key, style: Style): string {
  if (style === 'usi') return key;
  else return `${renderFile(key[0], style)} ${renderRank(key[1], style)} `;
}
