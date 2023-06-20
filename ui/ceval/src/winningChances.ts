import { Eval } from './types';

const toPov = (color: Color, diff: number): number => (color === 'white' ? diff : -diff);

/**
 * https://github.com/lichess-org/lila/pull/11148
 */
const rawWinningChances = (cp: number): number => {
  const MULTIPLIER = -0.00368208; // https://github.com/lichess-org/lila/pull/11148
  return 2 / (1 + Math.exp(MULTIPLIER * cp)) - 1;
};

const cpWinningChances = (cp: number): number => rawWinningChances(Math.min(Math.max(-1000, cp), 1000));

const mateWinningChances = (mate: number): number => {
  const cp = (21 - Math.min(10, Math.abs(mate))) * 100;
  const signed = cp * (mate > 0 ? 1 : -1);
  return rawWinningChances(signed);
};

const evalWinningChances = (ev: Eval): number =>
  typeof ev.mate !== 'undefined' ? mateWinningChances(ev.mate) : cpWinningChances(ev.cp!);

// winning chances for a color
// 1  infinitely winning
// -1 infinitely losing
export const povChances = (color: Color, ev: Eval): number => toPov(color, evalWinningChances(ev));

// computes the difference, in winning chances, between two evaluations
// 1  = e1 is infinitely better than e2
// -1 = e1 is infinitely worse  than e2
export const povDiff = (color: Color, e1: Eval, e2: Eval): number =>
  (povChances(color, e1) - povChances(color, e2)) / 2;
