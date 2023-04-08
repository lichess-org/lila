import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { CheckCount, CheckState, MaterialDiff } from './interfaces';

const ROLES: { [key: string]: cg.Role | undefined } = {
  p: 'pawn',
  n: 'knight',
  b: 'bishop',
  r: 'rook',
  q: 'queen',
  k: 'king',
};

export function getMaterialDiff(fenLike: string): MaterialDiff {
  const diff: MaterialDiff = {
    white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
    black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  };
  for (let i = 0, part = 0; i < fenLike.length && part < 8; i++) {
    const ch = fenLike[i];
    const lower = ch.toLowerCase();
    const role = ROLES[lower];
    if (role) {
      const color = ch == lower ? 'black' : 'white';
      const them = diff[opposite(color)];
      if (them[role] > 0) them[role]--;
      else diff[color][role]++;
    } else if (ch == '[' || ch == ' ') break;
    else if (ch == '/') part++;
  }
  return diff;
}

export function getScore(diff: MaterialDiff): number {
  return (
    (diff.white.queen - diff.black.queen) * 9 +
    (diff.white.rook - diff.black.rook) * 5 +
    (diff.white.bishop - diff.black.bishop) * 3 +
    (diff.white.knight - diff.black.knight) * 3 +
    (diff.white.pawn - diff.black.pawn)
  );
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
