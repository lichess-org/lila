import { uciChar } from './uciChar';
import { shuffle } from 'common/algo';

export * from './sanWriter';

export const fixCrazySan = (san: San): San => (san[0] === 'P' ? san.slice(1) : san);

export const destsToUcis = (destMap: Dests): Uci[] =>
  Array.from(destMap).reduce<Uci[]>((acc, [orig, dests]) => acc.concat(dests.map(dest => orig + dest)), []);

export const readDests = (lines?: string): Dests | null =>
  lines
    ? lines.split(' ').reduce<Dests>((dests, line) => {
        dests.set(
          uciChar[line[0]],
          line
            .slice(1)
            .split('')
            .map(c => uciChar[c]),
        );
        return dests;
      }, new Map())
    : null;

export const readDrops = (line?: string | null): Key[] | null =>
  line ? (line.match(/.{2}/g) as Key[]) || [] : null;

// Extended Position Description
export const fenToEpd = (fen: FEN): string => fen.split(' ').slice(0, 4).join(' ');

export const plyToTurn = (ply: number): number => Math.floor((ply - 1) / 2) + 1;

export const pieceCount = (fen: FEN): number => fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1;

export function fen960(): string {
  // a bit slower but more compact than array[960]
  const bishops = [2 * Math.floor(Math.random() * 4), 1 + 2 * Math.floor(Math.random() * 4)];
  const files = shuffle([0, 1, 2, 3, 4, 5, 6, 7]).filter(f => !bishops.includes(f));
  const [leftRook, king, rightRook] = files.slice(0, 3).sort();
  const [queen, knight1, knight2] = files.slice(3);
  const board = Array(8);
  board[bishops[0]] = board[bishops[1]] = 'b';
  board[leftRook] = board[rightRook] = 'r';
  board[king] = 'k';
  board[queen] = 'q';
  board[knight1] = board[knight2] = 'n';
  return `${board.join('')}/pppppppp/8/8/8/8/PPPPPPPP/${board.join('').toUpperCase()}`;
}
