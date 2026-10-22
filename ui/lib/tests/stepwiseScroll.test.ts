import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import stepwiseScroll from '../src/view/stepwiseScroll';

const wheel = (deltaY: number, deltaMode: number): WheelEvent =>
  new window.WheelEvent('wheel', { deltaY, deltaMode, cancelable: true });

const stepsFor = (events: WheelEvent[]): number => {
  let steps = 0;
  const onWheel = stepwiseScroll(
    () => steps++,
    () => false,
  );
  events.forEach(onWheel);
  return steps;
};

describe('stepwiseScroll', () => {
  test('one click of a classic wheel steps once', () => {
    assert.equal(stepsFor([wheel(120, 0)]), 1);
    assert.equal(stepsFor([wheel(100, 0)]), 1);
    assert.equal(stepsFor([wheel(-100, 0)]), 1);
    assert.equal(stepsFor([wheel(3, 1)]), 1);
    assert.equal(stepsFor([wheel(-3, 1)]), 1);
    assert.equal(stepsFor([wheel(1, 2)]), 1);
  });

  test('one click of a high resolution wheel steps once in pixel mode', () => {
    const click = [60, 15, 15, 15, 15].map(d => wheel(d, 0));
    assert.equal(stepsFor(click), 1);
    assert.equal(stepsFor([...click, ...click, ...click]), 3);
  });

  test('one click of a high resolution wheel steps once in line mode', () => {
    const click = [1.5, 0.375, 0.375, 0.375, 0.375].map(d => wheel(d, 1));
    assert.equal(stepsFor(click), 1);
    assert.equal(stepsFor([...click, ...click, ...click]), 3);
  });

  test('reversing direction steps on the first click back', () => {
    const down = [60, 15, 15, 15, 15].map(d => wheel(d, 0));
    const up = [-60, -15, -15, -15, -15].map(d => wheel(d, 0));
    let plies = 0;
    const onWheel = stepwiseScroll(
      e => (plies += Math.sign(e.deltaY)),
      () => false,
    );
    [...down, ...down, ...down, ...down].forEach(onWheel);
    assert.equal(plies, 4);
    up.forEach(onWheel);
    assert.equal(plies, 3);
  });

  test('skips and ctrl-wheel never step', () => {
    let steps = 0;
    const skipped = wheel(120, 0);
    stepwiseScroll(
      () => steps++,
      e => e === skipped,
      true,
    )(skipped);
    assert.equal(skipped.defaultPrevented, true);
    const zoom = new window.WheelEvent('wheel', {
      deltaY: 120,
      deltaMode: 0,
      ctrlKey: true,
      cancelable: true,
    });
    stepwiseScroll(
      () => steps++,
      () => false,
    )(zoom);
    assert.equal(zoom.defaultPrevented, false);
    assert.equal(steps, 0);
  });
});
