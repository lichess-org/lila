/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { destCharToKey } from './destCharToKey';
import { shuffle } from '../algo';
import { normalizeMove } from 'chessops/chess';
import { type Chess, type NormalMove, parseUci, makeUci } from 'chessops';

export const fixCrazySan = (san: San): San => (san[0] === 'P' ? san.slice(1) : san);

export const destsToUcis = (destMap: Dests): Uci[] =>
  Array.from(destMap).reduce<Uci[]>((acc, [orig, dests]) => acc.concat(dests.map(dest => orig + dest)), []);

export { uciToMove } from '@lichess-org/chessground/util';

export const fenColor = (fen: string): Color => (fen.includes(' w') ? 'white' : 'black');

export const readDests = (lines?: string): Dests | null => {
  if (lines == null) return null; // TODO: technically works, but should this be `=== undefined`?
  if (lines === '') return new Map();
  return lines.split(' ').reduce<Dests>((dests, line) => {
    dests.set(
      destCharToKey[line[0]],
      line
        .slice(1)
        .split('')
        .map(c => destCharToKey[c]),
    );
    return dests;
  }, new Map());
};

export const readDrops = (line?: string | null): Key[] | null =>
  line ? (line.match(/.{2}/g) as Key[]) || [] : null;

// Extended Position Description
export const fenToEpd = (fen: FEN): string => fen.split(' ').slice(0, 4).join(' ');

export const plyToTurn = (ply: number): number => Math.floor((ply - 1) / 2) + 1;

export const pieceCount = (fen: FEN): number => fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1;

export function fen960(): string {
  const [dark, light] = [2 * Math.floor(Math.random() * 4), 1 + 2 * Math.floor(Math.random() * 4)];
  const files = shuffle([0, 1, 2, 3, 4, 5, 6, 7].filter(f => f !== dark && f !== light));
  const [leftRook, king, rightRook] = files.slice(0, 3).sort();
  const [queen, knight1, knight2] = files.slice(3);
  const board = Array(8);
  board[dark] = board[light] = 'b';
  board[leftRook] = board[rightRook] = 'r';
  board[king] = 'k';
  board[queen] = 'q';
  board[knight1] = board[knight2] = 'n';
  return `${board.join('')}/pppppppp/8/8/8/8/PPPPPPPP/${board.join('').toUpperCase()}`;
}

export function normalMove(chess: Chess, uci: Uci): { uci: Uci; move: NormalMove } | undefined {
  const bareMove = parseUci(uci);
  const move =
    bareMove && 'from' in bareMove ? { ...bareMove, ...normalizeMove(chess, bareMove) } : undefined;
  return move && chess.isLegal(move) ? { uci: makeUci(move), move } : undefined;
}

export function isUci(maybeUci: string | undefined | null): maybeUci is Uci {
  return !!parseUci(maybeUci ?? '');
}

export function validUci(maybeUci: string | undefined | null): Uci | undefined {
  return isUci(maybeUci) ? maybeUci : undefined;
}
