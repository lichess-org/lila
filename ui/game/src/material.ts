import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { CheckCount, CheckState, MaterialDiff } from './interfaces';

const PIECE_SCORES = {
  pawn: 1,
  knight: 3,
  bishop: 3,
  rook: 5,
  queen: 9,
  king: 0,
};

export function getMaterialDiff(pieces: cg.Pieces): MaterialDiff {
  const diff: MaterialDiff = {
    white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
    black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  };
  for (const p of pieces.values()) {
    const them = diff[opposite(p.color)];
    if (them[p.role] > 0) them[p.role]--;
    else diff[p.color][p.role]++;
  }
  return diff;
}

export function getScore(pieces: cg.Pieces): number {
  let score = 0;
  for (const p of pieces.values()) {
    score += PIECE_SCORES[p.role] * (p.color === 'white' ? 1 : -1);
  }
  return score;
}

export const NO_CHECKS: CheckCount = {
  white: 0,
  black: 0,
};

export function countChecks(steps: CheckState[], ply: Ply): CheckCount {
  const checks: CheckCount = { ...NO_CHECKS };
  for (const step of steps) {
    if (ply < step.ply) break;
    if (step.check) {
      if (step.ply % 2 === 1) checks.white++;
      else checks.black++;
    }
  }
  return checks;
}
