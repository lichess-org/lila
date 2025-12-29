/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { charToRole, makeSquare, type Square } from 'chessops';
import { fixCrazySan } from './chess';

type AlmostSan = string;

export type Board = { pieces: { [key: number]: string }; turn: boolean };
export type SanToUci = { [key: AlmostSan]: Uci };

function decomposeUci(uci: string) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
}

export function square(name: string): Square {
  return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
}

export function squareDist(a: number, b: number): number {
  const x1 = a & 7,
    x2 = b & 7;
  const y1 = a >> 3,
    y2 = b >> 3;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function isBlack(p: string) {
  return p === p.toLowerCase();
}

export function readFen(fen: string): Board {
  const parts = fen.split(' '),
    board: Board = {
      pieces: {},
      turn: parts[1] === 'w',
    };

  parts[0]
    .split('/')
    .slice(0, 8)
    .forEach((row, y) => {
      let x = 0;
      row.split('').forEach(v => {
        if (v === '~') return;
        const nb = parseInt(v, 10);
        if (nb) x += nb;
        else {
          board.pieces[(7 - y) * 8 + x] = v;
          x++;
        }
      });
    });

  return board;
}

function kingMovesTo(s: number) {
  return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) === 1;
  });
}

function knightMovesTo(s: number) {
  return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) <= 2;
  });
}

const ROOK_DELTAS = [8, 1, -8, -1];
const BISHOP_DELTAS = [9, -9, 7, -7];
const QUEEN_DELTAS = [...ROOK_DELTAS, ...BISHOP_DELTAS];

function slidingMovesTo(s: number, deltas: number[], board: Board): number[] {
  const result: number[] = [];
  deltas.forEach(function (delta) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && squareDist(square, square - delta) === 1;
      square += delta
    ) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  });
  return result;
}

/* Produces a string that resembles a SAN,
 * but lacks the check/checkmate flag,
 * and probably has incomplete disambiguation.
 * But it's quick. */
export function almostSanOf(
  board: Board,
  uci: string,
  legalUcis: Set<Uci> | undefined = undefined,
): AlmostSan {
  if (uci.includes('@')) return fixCrazySan(uci);

  const move = decomposeUci(uci);
  const from = square(move[0]);
  const to = square(move[1]);
  const p = board.pieces[from];
  const d = board.pieces[to];
  const pt = board.pieces[from].toLowerCase();

  // pawn moves
  if (pt === 'p') {
    let san: AlmostSan;
    if (uci[0] === uci[2]) san = move[1];
    else san = uci[0] + 'x' + move[1];
    if (move[2]) san += '=' + move[2].toUpperCase();
    return san;
  }

  // castling
  if (pt === 'k' && ((d && isBlack(p) === isBlack(d)) || squareDist(from, to) > 1)) {
    if (to < from) return 'O-O-O';
    else return 'O-O';
  }

  let san = pt.toUpperCase();

  // disambiguate normal moves
  let candidates: number[] = [];
  if (pt === 'k') candidates = kingMovesTo(to);
  else if (pt === 'n') candidates = knightMovesTo(to);
  else if (pt === 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt === 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);
  else if (pt === 'q') candidates = slidingMovesTo(to, QUEEN_DELTAS, board);

  let rank = false,
    file = false;
  for (let i = 0; i < candidates.length; i++) {
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if (legalUcis && !legalUcis.has(makeSquare(candidates[i]) + move[1])) continue;
    if (from >> 3 === candidates[i] >> 3) file = true;
    if ((from & 7) === (candidates[i] & 7)) rank = true;
    else file = true;
  }
  if (file) san += uci[0];
  if (rank) san += uci[1];

  // target
  if (d) san += 'x';
  san += move[1];
  return san;
}

export function sanWriter(fen: string, ucis: Uci[]): SanToUci {
  const board = readFen(fen);
  const sans: SanToUci = {};
  const legalUcis = new Set(ucis);
  ucis.forEach(function (uci) {
    const san = almostSanOf(board, uci, legalUcis);
    sans[san] = uci;
    if (san.includes('x')) sans[san.replace('x', '')] = uci;
  });
  return sans;
}

export function sanToUci(san: string, legalSans: SanToUci): Uci | undefined {
  if (san in legalSans) return legalSans[san];
  const lowered = san.toLowerCase();
  for (const i in legalSans) if (i.toLowerCase() === lowered) return legalSans[i];
  return;
}

const sanToWords = (san: string): string =>
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

export function speakable(san?: San): string {
  const text = !san
    ? i18n.nvui.gameStart
    : sanToWords(san)
        .replace(/^A /, '"A"') // "A takes" & "A 3" are mispronounced
        .replace(/(\d) E (\d)/, '$1,E $2') // Strings such as 1E5 are treated as scientific notation
        .replace(/C /, 'c ') // Capital C is pronounced as "degrees celsius" when it comes after a number (e.g. R8c3)
        .replace(/F /, 'f ') // Capital F is pronounced as "degrees fahrenheit" when it comes after a number (e.g. R8f3)
        .replace(/(\d) H (\d)/, '$1H$2') // "H" is pronounced as "hour" when it comes after a number with a space (e.g. Rook 5 H 3)
        .replace(/(\d) H (\d)/, '$1H$2');
  return text;
}
