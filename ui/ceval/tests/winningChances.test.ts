import { describe, expect, test } from 'vitest';
import * as winningChances from '../src/winningChances';

const similarEvalsCp = (color: Color, bestEval: number, secondBestEval: number): boolean => {
  const toCp = (x: number) => {
    return { cp: x, mate: undefined };
  };
  return winningChances.areSimilarEvals(color, toCp(bestEval), toCp(secondBestEval));
};

describe('similarEvals', () => {
  // taken from https://github.com/lichess-org/tactics/issues/101
  test.each<[Color, number, number]>([
    ['black', -9600, -3500],
    ['white', 400, 350],
    ['black', -650, -630],
    ['black', -560, -460],
    ['black', -850, -640],
    ['black', -6500, -600],
    ['white', 400, 350],
    ['black', -6500, -6300],
    ['black', -560, -460],
    ['black', -850, -640],
    ['black', -6510, -600],
  ])('be similar', (color, bestEval, secondBestEval) => {
    expect(similarEvalsCp(color, bestEval, secondBestEval)).toBe(true);
  });

  // taken from the list of reported puzzles on zulip, and subjectively considered
  // false positives
  test.each<[Color, number, number]>([
    ['white', 265, -3],
    ['white', 269, 0],
    ['white', 322, -6],
    ['white', 778, 169],
    ['black', -293, -9],
    ['black', -179, 61],
    ['black', -816, -357],
  ])('be different', (color, bestEval, secondBestEval) => {
    expect(similarEvalsCp(color, bestEval, secondBestEval)).toBe(false);
  });

  // https://lichess.org/training/ZIRBc
  // It is unclear if this should be a false positive, but discussing with a few members
  // seems to be good enough to be considered a fp for now.
  test.each<[Color, EvalScore, EvalScore]>([
    ['black', { cp: undefined, mate: -16 }, { cp: -420, mate: undefined }],
  ])('be different mate/cp', (color, bestEval, secondBestEval) => {
    expect(winningChances.areSimilarEvals(color, bestEval, secondBestEval)).toBe(false);
  });
});
