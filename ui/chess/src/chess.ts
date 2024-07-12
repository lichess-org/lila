import { FEN } from 'chessground/types';
import { uciChar } from './uciChar';

export * from './sanWriter';
export { type MoveRootCtrl } from './moveRootCtrl';
export const initialFen: FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

export function fixCrazySan(san: San): San {
  return san[0] === 'P' ? san.slice(1) : san;
}

export type Dests = Map<Key, Key[]>;

export function destsToUcis(dests: Dests): Uci[] {
  const ucis: string[] = [];
  for (const [orig, d] of dests) {
    d.forEach(function (dest) {
      ucis.push(orig + dest);
    });
  }
  return ucis;
}

export function readDests(lines?: string): Dests | null {
  if (typeof lines === 'undefined') return null;
  const dests = new Map();
  if (lines)
    for (const line of lines.split(' ')) {
      dests.set(
        uciChar[line[0]],
        line
          .slice(1)
          .split('')
          .map(c => uciChar[c]),
      );
    }
  return dests;
}

export function readDrops(line?: string | null): Key[] | null {
  if (typeof line === 'undefined' || line === null) return null;
  return (line.match(/.{2}/g) as Key[]) || [];
}

export const altCastles = {
  e1a1: 'e1c1',
  e1h1: 'e1g1',
  e8a8: 'e8c8',
  e8h8: 'e8g8',
};

// we must strive to redefine roles and promotions in each and every module
export const from = (uci: Uci) => uci.slice(0, 2) as Key;
export const to = (uci: Uci) => uci.slice(2, 4) as Key;

export const Roles = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'] as const;
export type Role = (typeof Roles)[number];

export const RoleChars = ['K', 'Q', 'R', 'B', 'N', 'P'] as const;
export type RoleChar = (typeof RoleChars)[number];

export const roleChar = (role: string) =>
  ({
    king: 'K',
    queen: 'Q',
    rook: 'R',
    bishop: 'B',
    knight: 'N',
    pawn: 'P',
  })[role] as RoleChar;
export const charRole = (char: string) =>
  ({
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen',
    K: 'king',
  })[char] as Role;
export const promo = (uci: Uci): Role => charRole(uci.slice(4, 5).toUpperCase() as RoleChar);
