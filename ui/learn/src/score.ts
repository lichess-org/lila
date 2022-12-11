import type { PieceType } from 'chess.js';
import { Level, Stage } from './stage/list';
import * as util from './util';

export const apple = 50;
export const capture = 50;
export const scenario = 50;

const levelBonus = {
  1: 500,
  2: 300,
  3: 100,
};

export function getLevelBonus(l: Level, nbMoves: number) {
  const late = nbMoves - l.nbMoves;
  if (late <= 0) return levelBonus[1];
  if (late <= Math.max(1, l.nbMoves / 8)) return levelBonus[2];
  return levelBonus[3];
}

function getLevelMaxScore(l: Level) {
  let score = util.readKeys(l.apples).length * apple;
  if (l.pointsForCapture) score += (l.captures || 0) * capture;
  return score + levelBonus[1];
}

export function getLevelRank(l: Level, score: number) {
  const max = getLevelMaxScore(l);
  if (score >= max) return 1;
  if (score >= max - 200) return 2;
  return 3;
}

function getStageMaxScore(s: Stage) {
  return s.levels.reduce(function (sum, s) {
    return sum + getLevelMaxScore(s);
  }, 0);
}

export function getStageRank(s: Stage, score: number | number[]) {
  const max = getStageMaxScore(s);
  if (typeof score !== 'number') score = score.reduce((a, b) => a + b, 0);
  if (score >= max) return 1;
  if (score >= max - Math.max(200, s.levels.length * 150)) return 2;
  return 3;
}

const pieceValues = {
  q: 90,
  r: 50,
  b: 30,
  n: 30,
  p: 10,
};

export function pieceValue(p: Exclude<PieceType, 'k'>) {
  return pieceValues[p] || 0;
}

export function gtz(s: number) {
  return s > 0;
}
