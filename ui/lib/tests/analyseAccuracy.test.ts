import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import {
  formatPhaseAccuracy,
  gameAccuracy,
  hasPhaseDivision,
  phaseAccuracies,
  phaseAccuraciesDisplay,
  phaseOf,
  phaseAccuracy,
} from '../src/analyseAccuracy';

const isCloseTo = (actual: number, expected: number, tolerance: number) =>
  Math.abs(actual - expected) <= tolerance;

const compute = (cps: number[]) => gameAccuracy('white', cps);
const computeBlack = (cps: number[]) => gameAccuracy('black', cps);

describe('gameAccuracy', () => {
  test('empty game', () => {
    assert.strictEqual(compute([]), undefined);
  });

  test('single move', () => {
    assert.strictEqual(compute([15]), undefined);
  });

  test('two good moves', () => {
    const a = compute([15, 15])!;
    assert(isCloseTo(a.white!, 100, 1));
    assert(isCloseTo(a.black!, 100, 1));
  });

  test('white blunders on first move', () => {
    const a = compute([-900, -900])!;
    assert(isCloseTo(a.white!, 10, 5));
    assert(isCloseTo(a.black!, 100, 1));
  });

  test('black blunders on first move', () => {
    const a = compute([15, 900])!;
    assert(isCloseTo(a.white!, 100, 1));
    assert(isCloseTo(a.black!, 10, 5));
  });

  test('both blunder on first move', () => {
    const a = compute([-900, 0])!;
    assert(isCloseTo(a.white!, 10, 5));
    assert(isCloseTo(a.black!, 10, 5));
  });

  test('20 perfect moves', () => {
    const a = compute(Array(20).fill(15))!;
    assert(isCloseTo(a.white!, 100, 1));
    assert(isCloseTo(a.black!, 100, 1));
  });

  test('20 perfect moves and a white blunder', () => {
    const a = compute([...Array(20).fill(15), -900])!;
    assert(isCloseTo(a.white!, 50, 5));
    assert(isCloseTo(a.black!, 100, 1));
  });

  test('21 perfect moves and a black blunder', () => {
    const a = compute([...Array(21).fill(15), 900])!;
    assert(isCloseTo(a.white!, 100, 1));
    assert(isCloseTo(a.black!, 50, 5));
  });

  test('5 average moves (65 cpl) on each side', () => {
    const cps = Array.from({ length: 5 }, () => [-50, 15]).flat();
    const a = compute(cps)!;
    assert(isCloseTo(a.white!, 76, 8));
    assert(isCloseTo(a.black!, 76, 8));
  });

  test('50 average moves (65 cpl) on each side', () => {
    const cps = Array.from({ length: 50 }, () => [-50, 15]).flat();
    const a = compute(cps)!;
    assert(isCloseTo(a.white!, 76, 8));
    assert(isCloseTo(a.black!, 76, 8));
  });

  test('50 mediocre moves (150 cpl) on each side', () => {
    const cps = Array.from({ length: 50 }, () => [-135, 15]).flat();
    const a = compute(cps)!;
    assert(isCloseTo(a.white!, 54, 8));
    assert(isCloseTo(a.black!, 54, 8));
  });

  test('50 terrible moves (500 cpl) on each side', () => {
    const cps = Array.from({ length: 50 }, () => [-435, 15]).flat();
    const a = compute(cps)!;
    assert(isCloseTo(a.white!, 20, 8));
    assert(isCloseTo(a.black!, 20, 8));
  });
});

describe('gameAccuracy black moves first', () => {
  test('empty game', () => {
    assert.strictEqual(computeBlack([]), undefined);
  });

  test('single move', () => {
    assert.strictEqual(computeBlack([15]), undefined);
  });

  test('two good moves', () => {
    const a = computeBlack([15, 15])!;
    assert(isCloseTo(a.black!, 100, 1));
    assert(isCloseTo(a.white!, 100, 1));
  });

  test('black blunders on first move', () => {
    const a = computeBlack([900, 900])!;
    assert(isCloseTo(a.black!, 10, 5));
    assert(isCloseTo(a.white!, 100, 1));
  });

  test('white blunders on first move', () => {
    const a = computeBlack([15, -900])!;
    assert(isCloseTo(a.black!, 100, 1));
    assert(isCloseTo(a.white!, 10, 5));
  });

  test('both blunder on first move', () => {
    const a = computeBlack([900, 0])!;
    assert(isCloseTo(a.black!, 10, 5));
    assert(isCloseTo(a.white!, 10, 5));
  });
});

describe('phaseOf', () => {
  const div = { middle: 21, end: 41 };

  test('opening', () => {
    assert.strictEqual(phaseOf(div, 20), 'opening');
  });

  test('middlegame', () => {
    assert.strictEqual(phaseOf(div, 21), 'middlegame');
    assert.strictEqual(phaseOf(div, 40), 'middlegame');
  });

  test('endgame', () => {
    assert.strictEqual(phaseOf(div, 41), 'endgame');
  });

  test('no division is opening', () => {
    assert.strictEqual(phaseOf(undefined, 10), 'opening');
  });
});

describe('hasPhaseDivision', () => {
  test('requires middle above 1', () => {
    assert.strictEqual(hasPhaseDivision(undefined), false);
    assert.strictEqual(hasPhaseDivision({ middle: 1 }), false);
    assert.strictEqual(hasPhaseDivision({ middle: 21, end: 41 }), true);
  });
});

describe('phaseAccuracy', () => {
  const mainline = (plies: number[]) => [
    { ply: 0 },
    ...plies.map((ply, i) => ({
      ply,
      eval: { cp: i % 2 === 0 ? 15 : -50 },
      san: `${ply}`,
    })),
  ];

  test('splits by division', () => {
    const game = {
      division: { middle: 5, end: 9 },
      variant: { key: 'standard' },
    };
    const line = mainline([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    const phases = phaseAccuracies(line, 10, game);
    assert(phases?.opening);
    assert(phases?.middlegame);
    assert(phases?.endgame);
    const openingAccuracy = phaseAccuracy(line, 10, game, 'opening');
    assert(openingAccuracy);
    assert.strictEqual(openingAccuracy.white, phases.opening.white);
  });

  test('returns undefined with too few moves', () => {
    const line = mainline([1]);
    assert.strictEqual(phaseAccuracies(line, 1, { variant: { key: 'standard' } }), undefined);
  });

  test('opening-only fallback without fens when enough moves', () => {
    const line = mainline([1, 2, 3, 4]);
    const phases = phaseAccuracies(line, 4, { variant: { key: 'standard' } });
    assert(phases?.opening);
    assert.strictEqual(phases?.middlegame, undefined);
  });

  test('display always includes all phases', () => {
    const line = mainline([1]);
    const view = phaseAccuraciesDisplay(line, 1, { variant: { key: 'standard' } });
    assert.strictEqual(view.phases.opening.white, undefined);
    assert.strictEqual(view.phases.middlegame.white, undefined);
    assert.strictEqual(view.phases.endgame.white, undefined);
    assert.strictEqual(view.showHint, true);
    assert.strictEqual(formatPhaseAccuracy(undefined), '–');
  });

  test('display shows hint while analysis is partial', () => {
    const game = {
      division: { middle: 5, end: 9 },
      variant: { key: 'standard' },
    };
    const line = mainline([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    const view = phaseAccuraciesDisplay(line, 10, game, true);
    assert(view.hasAny);
    assert.strictEqual(view.showHint, true);
  });

  test('opening-only fallback when divider finds no later phase', () => {
    const startFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
    const line = [
      { ply: 0, fen: startFen },
      ...Array.from({ length: 10 }, (_, i) => ({
        ply: i + 1,
        eval: { cp: i % 2 === 0 ? 15 : -50 },
        san: 'e4',
        fen: startFen,
      })),
    ];
    const phases = phaseAccuracies(line, 10, { variant: { key: 'standard' } });
    assert(phases?.opening);
    assert.strictEqual(phases?.middlegame, undefined);
    assert.strictEqual(phases?.endgame, undefined);
  });
});
