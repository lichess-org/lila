import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import { GameData } from 'game';
import { Pieces } from 'chessground/types';
import { invRanks } from 'chessground/util';
import { Setting, makeSetting } from './setting';
import { files } from 'chessground/types';

export type Style = 'uci' | 'san' | 'literate' | 'nato' | 'anna';

const nato: { [letter: string]: string } = { a: 'alpha', b: 'bravo', c: 'charlie', d: 'delta', e: 'echo', f: 'foxtrot', g: 'golf', h: 'hotel' };
const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };
const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['san', 'SAN: Nxf3'],
      ['uci', 'UCI: g1f3'],
      ['literate', 'Literate: knight takes f 3'],
      ['anna', 'Anna: knight takes felix 3'],
      ['nato', 'Nato: knight takes foxtrot 3']
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: window.lichess.storage.make('nvui.moveNotation')
  });
}

export function renderSan(san: San, uci: Uci | undefined, style: Style) {
  if (!san) return '';
  const has = window.lichess.fp.contains;
  let move: string;
  if (has(san, 'O-O-O')) move = 'long castling';
  else if (has(san, 'O-O')) move = 'short castling';
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
  if (has(san, '+')) move += ' check';
  if (has(san, '#')) move += ' checkmate';
  return move;
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
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

export function renderBoard(pieces: Pieces, pov: Color): string {
  const board = [[' ', ...files, ' ']];
  for(let rank of invRanks) {
    let line = [];
    for(let file of files) {
      let key = file + rank;
      const piece = pieces[key];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((file.charCodeAt(0) + rank) % 2 ? '-' : '+');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...files, ' ']);
  if (pov === 'black') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}

export function renderFile(f: string, style: Style): string {
  return style === 'nato' ? nato[f] : (style === 'anna' ? anna[f] : f);
}

export function renderKey(key: string, style: Style): string {
  return `${renderFile(key[0], style)} ${key[1]}`;
}
