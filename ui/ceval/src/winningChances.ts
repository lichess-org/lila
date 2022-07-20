import { Eval } from './types';

const toPov = (color: Color, diff: number): number => (color === 'sente' ? diff : -diff);

// https://graphsketch.com/?eqn1_color=1&eqn1_eqn=100+*+%282+%2F+%281+%2B+exp%28-0.005+*+x%29%29+-+1%29&eqn2_color=2&eqn2_eqn=100+*+%282+%2F+%281+%2B+exp%28-0.004+*+x%29%29+-+1%29&eqn3_color=3&eqn3_eqn=&eqn4_color=4&eqn4_eqn=&eqn5_color=5&eqn5_eqn=&eqn6_color=6&eqn6_eqn=&x_min=-1000&x_max=1000&y_min=-100&y_max=100&x_tick=100&y_tick=10&x_label_freq=2&y_label_freq=2&do_grid=0&do_grid=1&bold_labeled_lines=0&bold_labeled_lines=1&line_width=4&image_w=850&image_h=525
const rawWinningChances = (cp: number): number => {
  const MULTIPLIER = -0.0007;
  return 2 / (1 + Math.exp(MULTIPLIER * cp)) - 1;
};

const cpWinningChances = (cp: number): number => rawWinningChances(Math.min(Math.max(-5500, cp), 5500));

const mateWinningChances = (mate: number): number => {
  // note: the minimum value of cp here (i.e. when mate >= 20) must be above the range in cpWinningChances (5500)
  const cp = (80 - Math.min(20, Math.abs(mate))) * 100;
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
