import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { type Pieces, files } from '@lichess-org/chessground/types';
import { COLORS, RANK_NAMES, ROLES, type FileName } from 'chessops/types';
import { charToRole, roleToChar } from 'chessops/util';
import { plyToTurn } from '../game/chess';
import type { MoveStyle, PieceStyle, PositionStyle, PrefixStyle, BoardStyle } from './setting';

export const renderPieceStyle = (ch: string, pieceStyle: PieceStyle): string =>
  pieceStyle === 'letter'
    ? ch.toLowerCase()
    : pieceStyle === 'white uppercase letter'
      ? ch
      : pieceStyle === 'name'
        ? charToRole(ch)!
        : `${ch.replace('N', 'K').replace('n', 'k')}${charToRole(ch)!.slice(1)}`;

export const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle): `${Color} ` | 'w' | 'b' | '' =>
  prefixStyle === 'letter' ? (color[0] as 'w' | 'b') : prefixStyle === 'name' ? `${color} ` : '';

export const renderPieceStr = (
  ch: string,
  pieceStyle: PieceStyle,
  c: Color,
  prefixStyle: PrefixStyle,
): string =>
  `${renderPrefixStyle(c, prefixStyle)} ${renderPieceStyle(c === 'white' ? ch.toUpperCase() : ch, pieceStyle)}`;

export const renderSan = (san: San | undefined, uci: Uci | undefined, style: MoveStyle): string =>
  !san
    ? i18n.nvui.gameStart
    : style === 'uci'
      ? (uci ?? '')
      : style === 'san'
        ? san
        : transSanToWords(san)
            .split(' ')
            .map(f =>
              files.includes(f.toLowerCase() as FileName)
                ? renderFile(f.toLowerCase() as FileName, style)
                : f,
            )
            .join(' ');

export const renderPieces = (pieces: Pieces, style: MoveStyle): VNode =>
  h(
    'div.pieces',
    COLORS.map(color =>
      h(`div.${color}-pieces`, [h('h3', i18n.site[color]), renderPiecesByColor(pieces, style, color)]),
    ),
  );

export const renderPockets = (pockets: Tree.NodeCrazy['pockets']): VNode =>
  h(
    'div.pieces',
    COLORS.map((color, i) =>
      h(`div.${color}-pieces`, [h('h3', i18n.site[color]), `${pocketsStr(pockets[i])}` || '0']),
    ),
  );

export const pocketsStr = (pocket: Tree.CrazyPocket): string =>
  Object.entries(pocket)
    .map(([role, count]) => `${i18n.nvui[role as Role]}: ${count}`)
    .join(', ');

export function renderPieceKeys(pieces: Pieces, p: string, style: MoveStyle): string {
  const color: Color = p === p.toUpperCase() ? 'white' : 'black';
  if (p.toLowerCase() == 'a') return renderPiecesByColor(pieces, style, color);
  const role = charToRole(p)!;
  const keys = keysWithPiece(pieces, role, color);
  let pieceStr = transPieceStr(role, color, i18n);
  if (!pieceStr) {
    console.error(`Missing piece name for ${color} ${role}`);
    pieceStr = `${color} ${role}`;
  }
  return `${pieceStr}: ${keys.length ? keys.map(k => renderKey(k, style)).join(', ') : i18n.site.none}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: MoveStyle): string {
  const renderedKeysWithPiece = Array.from(pieces)
    .sort(([key1], [key2]) => key1.localeCompare(key2))
    .reduce<string[]>(
      (acc, [key, p]) =>
        key.includes(rankOrFile)
          ? acc.concat(`${renderKey(key, style)} ${transPieceStr(p.role, p.color, i18n)}`)
          : acc,
      [],
    );
  return renderedKeysWithPiece.length ? renderedKeysWithPiece.join(', ') : i18n.site.none;
}

export function renderBoard(
  pieces: Pieces,
  pov: Color,
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
  positionStyle: PositionStyle,
  boardStyle: BoardStyle,
): VNode {
  const doRankHeader = (rank: Ranks): VNode => h('th', { attrs: { scope: 'row' } }, rank);

  const doFileHeaders = (): VNode => {
    const ths = files.map(file => h('th', { attrs: { scope: 'col' } }, file));
    return h('tr', [h('td'), ...(pov === 'black' ? ths.reverse() : ths), h('td')]);
  };

  const renderPositionStyle = (rank: Ranks, file: Files, orig: string) =>
    positionStyle === 'before'
      ? file.toUpperCase() + rank + ' ' + orig
      : positionStyle === 'after'
        ? orig + ' ' + file.toUpperCase() + rank
        : orig;

  const doPieceButton = (
    rank: Ranks,
    file: Files,
    letter: string,
    color: Color | 'none',
    text: string,
  ): VNode =>
    h(
      'button',
      {
        attrs: { rank: rank, file: file, piece: letter.toLowerCase(), color: color, 'trap-bypass': true },
      },
      renderPositionStyle(rank, file, text),
    );

  const doPiece = (rank: Ranks, file: Files): VNode => {
    const key: Key = `${file}${rank}`;
    const piece = pieces.get(key);
    const pieceWrapper = boardStyle === 'table' ? 'td' : 'span';
    if (piece) {
      const roleCh = roleToChar(piece.role);
      const pieceText =
        pieceStyle === 'name' || pieceStyle === 'white uppercase name'
          ? transPieceStr(piece.role, piece.color, i18n)
          : renderPieceStr(roleCh, pieceStyle, piece.color, prefixStyle);
      return h(pieceWrapper, doPieceButton(rank, file, roleCh, piece.color, pieceText));
    } else {
      const plusOrMinus = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      return h(pieceWrapper, doPieceButton(rank, file, plusOrMinus, 'none', plusOrMinus));
    }
  };

  const doRank = (pov: Color, rank: Ranks): VNode => {
    const rankElements: VNode[] = [];
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    rankElements.push(...files.map(file => doPiece(rank, file)));
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    if (pov === 'black') rankElements.reverse();
    return h(boardStyle === 'table' ? 'tr' : 'div', rankElements);
  };

  const ranks: VNode[] = [];
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  ranks.push(
    ...RANK_NAMES.slice()
      .reverse()
      .map(rank => doRank(pov, rank)),
  );
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  if (pov === 'black') ranks.reverse();
  return h(boardStyle === 'table' ? 'table.board-wrapper' : 'div.board-wrapper', ranks);
}

export const renderFile = (f: Files, style: MoveStyle): string =>
  style === 'nato' ? nato[f] : style === 'anna' ? anna[f] : f;

export const renderKey = (key: Key, style: MoveStyle): string =>
  style === 'nato' || style === 'anna' ? `${renderFile(key[0] as Files, style)} ${key[1]}` : key;

export function castlingFlavours(input: string): string {
  switch (input.toLowerCase().replace(/[-\s]+/g, '')) {
    case 'oo':
    case '00':
      return 'o-o';
    case 'ooo':
    case '000':
      return 'o-o-o';
  }
  return input;
}

export function renderMainline(
  nodes: Tree.Node[],
  currentPath: Tree.Path,
  style: MoveStyle,
  withComments = true,
): VNodeChildren {
  const res: VNodeChildren = [];
  let path: Tree.Path = '';
  nodes.forEach(node => {
    if (!node.san || !node.uci) return;
    path += node.id;
    const content: VNodeChildren = [
      node.ply & 1 ? plyToTurn(node.ply) + '. ' : null,
      renderSan(node.san, node.uci, style),
    ];
    res.push(h('move', { attrs: { p: path }, class: { active: path === currentPath } }, content));
    if (withComments) res.push(renderComments(node, style));
    res.push(', ');
    if (node.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

export const renderComments = (node: Tree.Node, style: MoveStyle): string =>
  node.comments?.map(c => ` ${augmentLichessComment(c, style)}`).join('.') ?? '';

export const isKey = (maybeKey: string): maybeKey is Key => !!maybeKey.match(/^[a-h][1-8]$/);

export const keyFromAttrs = (el: HTMLElement): Key | undefined => {
  const maybeKey = `${el.getAttribute('file') ?? ''}${el.getAttribute('rank') ?? ''}`;
  return isKey(maybeKey) ? maybeKey : undefined;
};

export const pieceStr = (role: Role, color: Color): string => transPieceStr(role, color, i18n);

export const transPieceStr = (role: Role, color: Color, i18n: I18n): string =>
  i18n.nvui[`${color}${role.charAt(0).toUpperCase()}${role.slice(1)}` as keyof typeof i18n.nvui] as string;

const renderPiecesByColor = (pieces: Pieces, style: MoveStyle, color: Color): string => {
  return ROLES.slice()
    .reverse()
    .reduce<{ role: Role; keys: Key[] }[]>(
      (lists, role) =>
        lists.concat({
          role,
          keys: keysWithPiece(pieces, role, color),
        }),
      [],
    )
    .filter(l => l.keys.length)
    .map(l => `${transRole(l.role)}: ${l.keys.map(k => renderKey(k, style)).join(', ')}`)
    .join(', ');
};

const keysWithPiece = (pieces: Pieces, role?: Role, color?: Color): Key[] =>
  Array.from(pieces).reduce<Key[]>(
    (keys, [key, p]) => (p.color === color && p.role === role ? keys.concat(key) : keys),
    [],
  );

const augmentLichessComment = (comment: Tree.Comment, style: MoveStyle): string =>
  comment.by === 'lichess'
    ? comment.text.replace(
        /([^\s]+) was best\./,
        (_, san) => `Best move was ${renderSan(san, undefined, style)}`,
      )
    : comment.text;

const transSanToWords = (san: string): string =>
  san
    .split('')
    .map(c => {
      if (c === 'x') return i18n.nvui.sanTakes;
      if (c === '+') return i18n.nvui.sanCheck;
      if (c === '#') return i18n.nvui.sanCheckmate;
      if (c === '=') return i18n.nvui.sanPromotesTo;
      if (c === '@') return i18n.nvui.sanDroppedOn;
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c; // 1-8
      if (code > 96 && code < 105) return c.toUpperCase(); // a-h
      const role = charToRole(c);
      return role ? transRole(role) : c;
    })
    .join(' ')
    .replace('O - O - O', i18n.nvui.sanLongCastling)
    .replace('O - O', i18n.nvui.sanShortCastling);

const transRole = (role: Role): string =>
  (i18n.nvui[role as keyof typeof i18n.nvui] as string) || (role as string);

const nato: { [file in Files]: string } = {
  a: 'alpha',
  b: 'bravo',
  c: 'charlie',
  d: 'delta',
  e: 'echo',
  f: 'foxtrot',
  g: 'golf',
  h: 'hotel',
};
const anna: { [file in Files]: string } = {
  a: 'anna',
  b: 'bella',
  c: 'cesar',
  d: 'david',
  e: 'eva',
  f: 'felix',
  g: 'gustav',
  h: 'hector',
};
