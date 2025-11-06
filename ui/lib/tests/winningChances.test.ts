import { describe } from 'node:test';
import assert from 'node:assert/strict';
import * as winningChances from '../src/ceval/winningChances';
import { each } from '../../.test/helpers.mts';

type CentipawnsOrMate = number | string;

const toEv = (x: CentipawnsOrMate): EvalScore => {
  if (typeof x === 'number') return { cp: x, mate: undefined };
  if (typeof x === 'string' && x.startsWith('#')) return { cp: undefined, mate: parseInt(x.slice(1)) };
  throw new Error('Invalid input');
};

const similarEvalsCp = (
  color: Color,
  bestEval: CentipawnsOrMate,
  secondBestEval: CentipawnsOrMate,
): boolean => winningChances.areSimilarEvals(color, toEv(bestEval), toEv(secondBestEval));

const hasMultipleSolutionsCp = (
  color: Color,
  bestEval: CentipawnsOrMate,
  secondBestEval: CentipawnsOrMate,
): boolean => winningChances.hasMultipleSolutions(color, toEv(bestEval), toEv(secondBestEval));

describe('similarEvals', () => {
  // taken from https://github.com/lichess-org/tactics/issues/101
  each<[Color, number, number]>([
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
    assert.strictEqual(similarEvalsCp(color, bestEval, secondBestEval), true);
  });

  // taken from the list of reported puzzles on zulip, and subjectively considered false positives
  each<[Color, CentipawnsOrMate, CentipawnsOrMate]>([
    ['white', 265, -3],
    ['white', 269, 0],
    ['white', 322, -6],
    ['white', 778, 169],
    ['black', -293, -9],
    ['black', -179, 61],
    ['black', -816, -357],
    ['black', -225, -51],
  ])('be different', (color, bestEval, secondBestEval) => {
    assert.strictEqual(similarEvalsCp(color, bestEval, secondBestEval), false);
  });

  each<[Color, EvalScore, EvalScore]>([
    ['black', { cp: undefined, mate: -16 }, { cp: -420, mate: undefined }],
  ])('be different mate/cp', (color, bestEval, secondBestEval) => {
    assert.strictEqual(winningChances.areSimilarEvals(color, bestEval, secondBestEval), false);
  });
});

describe('hasMultipleSolutions', () => {
  each<[Color, CentipawnsOrMate, CentipawnsOrMate]>([
    // https://lichess.org/training/ZIRBc
    // It is unclear if this should not be a false positive
    // but try to report more puzzles like that for the moment to get more opinions
    ['black', '#-16', -420],
    ['white', 100000, 200],
  ])('be true', (color, bestEval, secondBestEval) => {
    assert.strictEqual(hasMultipleSolutionsCp(color, bestEval, secondBestEval), true);
  });
});
