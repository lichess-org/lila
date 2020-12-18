import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Pieces } from 'chessground/types';
import { Rank, File } from 'chessground/types';
import { invRanks, allKeys } from 'chessground/util';
import { Setting, makeSetting } from './setting';
import { files } from 'chessground/types';

export type Style = 'uci' | 'san' | 'literate' | 'nato' | 'anna';
export type PieceStyle = 'letter' | 'white uppercase letter' | 'name' | 'white uppercase name';
export type PrefixStyle = 'letter' | 'name' | 'none';
export type PositionStyle = 'before' | 'after' | 'none';

const nato: { [letter: string]: string } = { a: 'alpha', b: 'bravo', c: 'charlie', d: 'delta', e: 'echo', f: 'foxtrot', g: 'golf', h: 'hotel' };
const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };
const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

const letterPiece: { [letter: string]: string} = { p: 'p', r: 'r', n: 'n', b: 'b', q: 'q', k: 'k',
                                                   P: 'p', R: 'r', N: 'n', B: 'b', Q: 'q', K: 'k'};
const whiteUpperLetterPiece: { [letter: string]: string} = { p: 'p', r: 'r', n: 'n', b: 'b', q: 'q', k: 'k',
                                                   P: 'P', R: 'R', N: 'N', B: 'B', Q: 'Q', K: 'K'};
const namePiece: { [letter: string]: string} = { p: 'pawn', r: 'rook', n: 'knight', b: 'bishop', q: 'queen', k: 'king',
                                                   P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king'};
const whiteUpperNamePiece: { [letter: string]: string} = { p: 'pawn', r: 'rook', n: 'knight', b: 'bishop', q: 'queen', k: 'king',
                                                   P: 'Pawn', R: 'Rook', N: 'Knight', B: 'Bishop', Q: 'Queen', K: 'King'};
const skipToFile: { [letter: string]: string} = {'!': 'a', '@': 'b', '#': 'c', '$': 'd', '%': 'e', '^': 'f', '&': 'g', '*': 'h'};

export function symbolToFile(char: string)  {
  return skipToFile[char] ?? "";
}

export function supportedVariant(key: string) {
  return [
    'standard', 'chess960', 'kingOfTheHill', 'threeCheck', 'fromPosition'
  ].includes(key);
}

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['san', 'SAN: Nxf3'],
      ['uci', 'UCI: g1f3'],
      ['literate', 'Literate: knight takes f 3'],
      ['anna', 'Anna: knight takes felix 3'],
      ['nato', 'Nato: knight takes foxtrot 3'],
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: lichess.storage.make('nvui.moveNotation')
  });
}

export function pieceSetting(): Setting<PieceStyle> {
  return makeSetting<PieceStyle>({
    choices: [
      ['letter', 'Letter: p, p'],
      ['white uppercase letter', 'White uppecase letter: P, p'],
      ['name', 'Name: pawn, pawn'],
      ['white uppercase name', 'White uppercase name: Pawn, pawn']
    ],
    default: 'letter',
    storage: lichess.storage.make('nvui.pieceStyle')
  });
}

export function prefixSetting(): Setting<PrefixStyle> {
  return makeSetting<PrefixStyle>({
    choices: [
      ['letter', 'Letter: w/b'],
      ['name', 'Name: white/black'],
      ['none', 'None']
    ],
    default: 'letter',
    storage: lichess.storage.make('nvui.prefixStyle')
  });
}

export function positionSetting(): Setting<PositionStyle> {
  return makeSetting<PositionStyle>({
    choices: [
      ['before', 'before: c2: wp'],
      ['after', 'after: wp: c2'],
      ['none', 'none']
    ],
    default: 'before',
    storage: lichess.storage.make('nvui.positionStyle')
  });
}
const renderPieceStyle = (piece: string, pieceStyle: PieceStyle) => {
  switch(pieceStyle) {
    case 'letter':
      return letterPiece[piece];
    case 'white uppercase letter':
      return whiteUpperLetterPiece[piece];
    case 'name':
      return namePiece[piece];
    case 'white uppercase name':
      return whiteUpperNamePiece[piece];
  }
}
const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle) => {
  switch(prefixStyle) {
    case 'letter':
      return color.charAt(0);
    case 'name':
      return color + " ";
    case 'none':
      return '';
  }
}

export function takes(moves: string[], pieceStyle: PieceStyle, prefixStyle: PrefixStyle) {
  const oldFen = moves[moves.length-2].split(' ')[0];
  const newFen = moves[moves.length-1].split(' ')[0];
  for (var p of 'kKqQrRbBnNpP') {
    const diff = (oldFen.split(p).length - 1) - (newFen.split(p).length -1);
    if (diff == 1) {
      console.log(renderPrefixStyle(p.toUpperCase() === p ? "white" : "black", prefixStyle) +
      renderPieceStyle(p, pieceStyle));
    }
  }
}

export function renderSan(san: San, uci: Uci | undefined, style: Style) {
  if (!san) return '';
  let move: string;
  if (san.includes('O-O-O')) move = 'long castling';
  else if (san.includes('O-O')) move = 'short castling';
  else if (style === 'san') move = san.replace(/[\+#]/, '');
  else if (style === 'uci') move = uci || san;
  else {
    move = san.replace(/[\+#]/, '').split('').map(c => {
      if (c == 'x') return 'takes';
      if (c == '+') return 'check';
      if (c == '#') return 'checkmate';
      if (c == '=') return 'promotion';
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c; // 1-8
      if (code > 96 && code < 105) return renderFile(c, style); // a-g
      return roles[c] || c;
    }).join(' ');
  }
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (const [key, piece] of pieces) {
        if (piece.color === color && piece.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).map((k: string) => renderKey(k, style)).join(', ')}`
      ).join(', ')
    ]);
  }));
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const name = `${p === p.toUpperCase() ? 'white' : 'black'} ${roles[p.toUpperCase()]}`;
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

export function renderBoard(pieces: Pieces, pov: Color, pieceStyle: PieceStyle, prefixStyle: PrefixStyle, positionStyle: PositionStyle): VNode {
  const renderPositionStyle = (rank: Rank, file: File, orig: string) => {
    switch(positionStyle) {
      case 'before':
        return file.toUpperCase() + rank + ' ' + orig;
      case 'after':
        return orig + ' ' + file.toUpperCase() + rank;
      case 'none':
        return orig;
    }
  }
  const doPieceButton = (rank: Rank, file: File, letter: string, text: string): VNode => {
    return h('button', {
      attrs: { rank: rank, file: file, piece: letter.toLowerCase()}
    }, text);
  }
  const doPiece = (rank: Rank, file: File): VNode => {
    const key = file + rank as Key;
    const piece = pieces.get(key);
    if (piece) {        
      const letter = renderPieceStyle(piece.color === 'white' ? letters[piece.role].toUpperCase() : letters[piece.role], pieceStyle);
      const prefix = renderPrefixStyle(piece.color, prefixStyle);
      const text = renderPositionStyle(rank, file, prefix + letter);
      return h('span', doPieceButton(rank, file, letter, text));
    } else {
      const letter = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      const text = renderPositionStyle(rank, file, letter);
      return h('span', doPieceButton(rank, file, letter, text));
    }
  }
  const doRank = (pov: Color, rank: Rank): VNode => {      
    let rankElements = [
      //doRankHeader(rank),
      ...files.map(file => doPiece(rank, file)),
      //doRankHeader(rank)
    ];
    if (pov === 'black') rankElements.reverse();
    return h('div', rankElements);
  }
  let ranks = invRanks.map(rank => doRank(pov, rank));
  if (pov === 'black') ranks.reverse();
  return h('spam', [
    //doFileHeaders(pov),
    ...ranks,
    //doFileHeaders(pov)
  ]);
}

export function renderFile(f: string, style: Style): string {
  return style === 'nato' ? nato[f] : (style === 'anna' ? anna[f] : f);
}

export function renderKey(key: string, style: Style): string {
  return `${renderFile(key[0], style)} ${key[1]}`;
}

export function castlingFlavours(input: string): string {
  switch(input.toLowerCase().replace(/[-\s]+/g, '')) {
    case 'oo': case '00': return 'o-o';
    case 'ooo': case '000': return 'o-o-o';
  }
  return input;
}
