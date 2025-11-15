import { clamp } from '../algo';
import type { MoveArgs } from './types';

export function movetime(
  { initial, increment, remaining, opponentRemaining, ply }: MoveArgs,
  rating: number,
): Seconds {
  const meanMovetimeAt = models.get(rating) ?? makeMovetimer(initial, increment, rating);
  models.set(rating, meanMovetimeAt);
  if (!Number.isFinite(initial)) return 2;
  if (ply < 2) return 0;
  const target = meanMovetimeAt(ply / 2 - 1);

  // now shit all over our statistically accurate mean with situational adjustments
  const unitDrift = Math.sin(Math.PI * Math.random());

  return Math.random() * (remaining + opponentRemaining) < remaining // above or below target
    ? target * (1 + unitDrift)
    : target - unitDrift * (target - meanMovetimeAt(240));
}

const models = new Map<Seconds, (turn: number) => Seconds>();

// keyframes mined from jan 2025 https://database.lichess.org
const keyframes = new Map([
  [0, { first: { m: -0.0002, b: 0.6 }, peak: { m: -0.0003, b: 1.0 }, tail: { m: -0.00016, b: 0.46 } }],
  [15, { first: { m: -0.00027, b: 0.72 }, peak: { m: -0.00031, b: 1.11 }, tail: { m: -0.00015, b: 0.46 } }],
  [30, { first: { m: -0.00042, b: 1.16 }, peak: { m: -0.00032, b: 1.69 }, tail: { m: -0.00013, b: 0.45 } }],
  [60, { first: { m: -0.00038, b: 1.33 }, peak: { m: -0.0006, b: 3.28 }, tail: { m: -0.00007, b: 0.34 } }],
  [180, { first: { m: -0.0005, b: 2.25 }, peak: { m: -0.0003, b: 5.7 }, tail: { m: 0, b: 1 } }],
  [300, { first: { m: -0.00065, b: 3.08 }, peak: { m: -0.00105, b: 8.9 }, tail: { m: 0, b: 1 } }],
  [600, { first: { m: -0.0008, b: 4.56 }, peak: { m: 0.00178, b: 9.5 }, tail: { m: 0, b: 1 } }],
  [3600, { first: { m: -0.00041, b: 9.36 }, peak: { m: 0.03, b: -13.3 }, tail: { m: 0, b: 1 } }],
]);

const keys = [...keyframes.keys()].sort();

type Line = { m: number; b: number };

function lineMix(from: Line, to: Line, mix: number): Line {
  return { m: from.m + (to.m - from.m) * mix, b: from.b + (to.b - from.b) * mix };
}

// mean movetime from turn with a sin^2 ramp up to peak and quadratic decay
function makeMovetimer(initial: Seconds, increment: Seconds, rating: number): (turn: number) => Seconds {
  const cappedInitial = clamp(initial, { min: keys[0], max: keys[keys.length - 1] });

  const index = keys.findIndex(i => i > cappedInitial);
  const lowerKey = index > 0 ? keys[index - 1] : keys[0];
  const upperKey = index > -1 ? keys[index] : keys[keys.length - 1];

  const mix = upperKey === lowerKey ? 0 : (initial - lowerKey) / (upperKey - lowerKey);
  const lowerFrame = keyframes.get(lowerKey)!;
  const upperFrame = keyframes.get(upperKey)!;

  const firstLine = lineMix(lowerFrame.first, upperFrame.first, mix);
  const tailLine = lineMix(lowerFrame.tail, upperFrame.tail, mix);
  const peakLine = lineMix(lowerFrame.peak, upperFrame.peak, mix);
  if (increment > 0) {
    peakLine.m += (increment - 1.3) * 0.0012;
    peakLine.b -= initial <= 180 ? 0 : increment * 0.25;
  }

  const firstTime = firstLine.m * rating + firstLine.b;
  const tailTime = tailLine.m * rating + tailLine.b;
  const peakTime = Math.max(tailTime, peakLine.m * rating + peakLine.b);

  const peakTurn =
    increment > 0
      ? Math.max(12 + ((2 - increment) * (rating - 1000)) / 1300, 7)
      : initial <= 60
        ? Math.min(4 + (initial / 15) ** 1.2 + ((15 - (initial / 15) ** 1.2) * (rating - 600)) / 1800, 19)
        : Math.min(12 + (initial - 180) / 60 + (rating - 600) / 138, 19);

  return (turn: number) => {
    if (turn <= peakTurn)
      return firstTime + (peakTime - firstTime) * Math.sin((Math.PI * turn) / peakTurn / 2) ** 2;
    else return tailTime + (peakTime - tailTime) / (1 + ((turn - peakTurn) / 15) ** 2);
  };
}
