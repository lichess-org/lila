// no side effects allowed due to re-export by index.ts

import type { WinningChances } from './types';

const toPov = (color: Color, diff: number): number => (color === 'white' ? diff : -diff);

/**
 * https://github.com/lichess-org/lila/pull/11148
 */
const rawWinningChances = (cp: number): WinningChances => {
  const MULTIPLIER = -0.00368208; // https://github.com/lichess-org/lila/pull/11148
  return 2 / (1 + Math.exp(MULTIPLIER * cp)) - 1;
};

const cpWinningChances = (cp: number): WinningChances =>
  rawWinningChances(Math.min(Math.max(-1000, cp), 1000));

const mateWinningChances = (mate: number): WinningChances => {
  const cp = (21 - Math.min(10, Math.abs(mate))) * 100;
  const signed = cp * (mate > 0 ? 1 : -1);
  return rawWinningChances(signed);
};

const evalWinningChances = (ev: EvalScore): WinningChances =>
  typeof ev.mate !== 'undefined' ? mateWinningChances(ev.mate) : cpWinningChances(ev.cp!);

// winning chances for a color
// 1  infinitely winning
// -1 infinitely losing
export const povChances = (color: Color, ev: EvalScore): WinningChances =>
  toPov(color, evalWinningChances(ev));

// computes the difference, in winning chances, between two evaluations
// 1  = e1 is infinitely better than e2
// -1 = e1 is infinitely worse  than e2
export const povDiff = (color: Color, e1: EvalScore, e2: EvalScore): number =>
  (povChances(color, e1) - povChances(color, e2)) / 2;

// used to check if two evaluations are similar enough
// to report puzzles as faulty
//
// stricter than lichess-puzzler v49 check
// to avoid false positives and only report really faulty puzzles
export const areSimilarEvals = (pov: Color, bestEval: EvalScore, secondBestEval: EvalScore): boolean => {
  return povDiff(pov, bestEval, secondBestEval) < 0.14;
};

// used to report puzzles as faulty
export const hasMultipleSolutions = (
  color: Color,
  bestEval: EvalScore,
  secondBestEval: EvalScore,
): boolean => {
  return (
    // if secondbest eval equivalent of cp is >= 200
    povChances(color, secondBestEval) >= 0.3524 || areSimilarEvals(color, bestEval, secondBestEval)
  );
};
