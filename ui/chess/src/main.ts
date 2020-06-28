import { piotr } from './piotr';

export const initialFen: Fen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

export function fixCrazySan(san: San): San {
  return san[0] === 'P' ? san.slice(1) : san;
}

export interface Dests {
  [square: string]: Key[];
}

export function readDests(lines?: string): Dests | null {
  if (typeof lines === 'undefined') return null;
  const dests: Dests = {};
  if (lines) lines.split(' ').forEach(line => {
    dests[piotr[line[0]]] = line.slice(1).split('').map(c => piotr[c]);
  });
  return dests;
}

export function readDrops(line?: string | null): string[] | null {
  if (typeof line === 'undefined' || line === null) return null;
  return line.match(/.{2}/g) || [];
}

export const altCastles = {
  e1a1: 'e1c1',
  e1h1: 'e1g1',
  e8a8: 'e8c8',
  e8h8: 'e8g8',
};
