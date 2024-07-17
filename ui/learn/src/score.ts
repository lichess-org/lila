import type { PieceType } from 'chess.js';
import { Level, Stage } from './stage/list';
import * as util from './util';

export const apple = 50;
export const capture = 50;
export const scenario = 50;

type Rank = 1 | 2 | 3;

const levelBonus: { [r in Rank]: number } = {
  1: 500,
  2: 300,
  3: 100,
};

export const getLevelBonus = (l: Level, nbMoves: number) => {
  const late = nbMoves - l.nbMoves;
  return late <= 0 ? levelBonus[1] : late <= Math.max(1, l.nbMoves / 8) ? levelBonus[2] : levelBonus[3];
};

const getLevelMaxScore = (l: Level): number =>
  util.readKeys(l.apples).length * apple +
  (l.pointsForCapture ? (l.captures || 0) * capture : 0) +
  levelBonus[1];

export const getLevelRank = (l: Level, score: number): Rank => {
  const max = getLevelMaxScore(l);
  return score >= max ? 1 : score >= max - 200 ? 2 : 3;
};

const getStageMaxScore = (s: Stage) => s.levels.reduce((sum, s) => sum + getLevelMaxScore(s), 0);

export const getStageRank = (s: Stage, score: number | number[]): Rank => {
  const max = getStageMaxScore(s);
  if (typeof score !== 'number') score = score.reduce((a, b) => a + b, 0);
  return score >= max ? 1 : score >= max - Math.max(200, s.levels.length * 150) ? 2 : 3;
};

type ExcludingKing = Exclude<PieceType, 'k'>;

const pieceValues: { [key in ExcludingKing]: number } = {
  q: 90,
  r: 50,
  b: 30,
  n: 30,
  p: 10,
};

export const pieceValue = (p: ExcludingKing) => pieceValues[p];

export const gtz = (s: number) => s > 0;
