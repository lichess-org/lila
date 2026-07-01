import type { Board } from 'chessops/board';
import { lichessRules } from 'chessops/compat';
import { parseFen } from 'chessops/fen';
import { SquareSet } from 'chessops/squareSet';
import { setupPosition } from 'chessops/variant';

import type { TreeNodeBase } from './tree/types';

export interface Division {
  middle?: number;
  end?: number;
}

const divisionVariants = new Set(['standard', 'chess960', 'fromPosition', 'kingOfTheHill', 'threeCheck']);

const firstRank = SquareSet.fromRank(0);
const lastRank = SquareSet.fromRank(7);

const regionAt = (x: number, y: number): SquareSet => {
  let set = SquareSet.empty();
  for (const dy of [0, 1]) {
    for (const dx of [0, 1]) {
      set = set.with((y + dy) * 8 + (x + dx));
    }
  }
  return set;
};

const mixednessRegions: { region: SquareSet; y: number }[] = (() => {
  const regions: { region: SquareSet; y: number }[] = [];
  for (let y = 0; y <= 6; y++) {
    for (let x = 0; x <= 6; x++) {
      regions.push({ region: regionAt(x, y), y: y + 1 });
    }
  }
  return regions;
})();

const score = (y: number, white: number, black: number): number => {
  if (white === 0 && black === 0) return 0;
  if (white === 1 && black === 0) return 1 + (8 - y);
  if (white === 2 && black === 0) return y > 2 ? 2 + (y - 2) : 0;
  if (white === 3 && black === 0) return y > 1 ? 3 + (y - 1) : 0;
  if (white === 4 && black === 0) return y > 1 ? 3 + (y - 1) : 0;
  if (white === 0 && black === 1) return 1 + y;
  if (white === 1 && black === 1) return 5 + Math.abs(4 - y);
  if (white === 2 && black === 1) return 4 + (y - 1);
  if (white === 3 && black === 1) return 5 + (y - 1);
  if (white === 0 && black === 2) return y < 6 ? 2 + (6 - y) : 0;
  if (white === 1 && black === 2) return 4 + (7 - y);
  if (white === 2 && black === 2) return 7;
  if (white === 0 && black === 3) return y < 7 ? 3 + (7 - y) : 0;
  if (white === 1 && black === 3) return 5 + (7 - y);
  if (white === 0 && black === 4) return y < 7 ? 3 + (7 - y) : 0;
  return 0;
};

const majorsAndMinors = (board: Board): number => board.occupied.diff(board.king.union(board.pawn)).size();

const backrankSparse = (board: Board): boolean =>
  board.white.intersect(firstRank).size() < 4 || board.black.intersect(lastRank).size() < 4;

const mixedness = (board: Board): number =>
  mixednessRegions.reduce((acc, { region, y }) => {
    const white = board.white.intersect(region).size();
    const black = board.black.intersect(region).size();
    return acc + score(y, white, black);
  }, 0);

const boardFromFen = (fen: string, variantKey: string): Board | undefined => {
  const setup = parseFen(fen);
  if (setup.isErr) return undefined;
  const pos = setupPosition(lichessRules(variantKey as VariantKey), setup.value);
  if (pos.isErr) return undefined;
  return pos.value.board;
};

/** Mirrors chess.Divider.apply for standard-like variants. */
export const divisionFromMainline = (
  mainline: TreeNodeBase[],
  variantKey = 'standard',
): Division | undefined => {
  if (!divisionVariants.has(variantKey)) return undefined;

  const boards: Board[] = [];
  for (const node of mainline) {
    if (!node.fen) return undefined;
    const board = boardFromFen(node.fen, variantKey);
    if (!board) return undefined;
    boards.push(board);
  }
  if (boards.length === 0) return undefined;

  let midGameIndex: number | undefined;
  for (let index = 0; index < boards.length; index++) {
    const board = boards[index];
    if (majorsAndMinors(board) <= 10 || backrankSparse(board) || mixedness(board) > 150) {
      midGameIndex = index;
      break;
    }
  }

  let endGameIndex: number | undefined;
  if (midGameIndex !== undefined) {
    for (let index = 0; index < boards.length; index++) {
      if (majorsAndMinors(boards[index]) <= 6) {
        endGameIndex = index;
        break;
      }
    }
  }

  const middleIndex =
    midGameIndex !== undefined && (endGameIndex === undefined || midGameIndex < endGameIndex)
      ? midGameIndex
      : undefined;
  const middle =
    middleIndex !== undefined && middleIndex < mainline.length ? mainline[middleIndex].ply : undefined;
  const end =
    endGameIndex !== undefined && endGameIndex < mainline.length ? mainline[endGameIndex].ply : undefined;

  if (middle === undefined || middle <= 1) return undefined;
  return { middle, end };
};

/** When the divider finds no later phase, treat the whole game as opening. */
export const openingOnlyDivision = (mainline: TreeNodeBase[]): Division | undefined => {
  const lastPly = mainline[mainline.length - 1]?.ply;
  if (lastPly === undefined || lastPly <= 1) return undefined;
  return { middle: lastPly + 1 };
};

export const effectiveDivision = (
  division: Division | undefined,
  mainline: TreeNodeBase[],
  variantKey?: string,
): Division | undefined => {
  if (division?.middle !== undefined && division.middle > 1) return division;

  const computed = divisionFromMainline(mainline, variantKey);
  if (computed?.middle !== undefined && computed.middle > 1) return computed;

  return openingOnlyDivision(mainline);
};
