import assert from 'node:assert/strict';
import { test } from 'node:test';

import { parseTimeToCentis, formatClockFromCentis } from '../src/study/studyClockEdit';

test('parseTimeToCentis: empty and placeholder', () => {
  assert.equal(parseTimeToCentis(''), undefined);
  assert.equal(parseTimeToCentis('   '), undefined);
  assert.equal(parseTimeToCentis('--:--'), undefined);
});

test('parseTimeToCentis: seconds only', () => {
  assert.equal(parseTimeToCentis('0'), 0);
  assert.equal(parseTimeToCentis('40'), 4000);
  assert.equal(parseTimeToCentis('59'), 5900);
  assert.equal(parseTimeToCentis('40.5'), 4050);
  assert.equal(parseTimeToCentis('3.7'), 370);
});

test('parseTimeToCentis: MM:SS', () => {
  assert.equal(parseTimeToCentis('3:40'), 22000);
  assert.equal(parseTimeToCentis('1:00'), 6000);
  assert.equal(parseTimeToCentis('0:33'), 3300);
  assert.equal(parseTimeToCentis('3:40.2'), 22020);
});

test('parseTimeToCentis: H:MM:SS', () => {
  assert.equal(parseTimeToCentis('1:03:40'), 382000);
  assert.equal(parseTimeToCentis('1:00:00'), 360000);
  assert.equal(parseTimeToCentis('0:01:30'), 9000);
});

test('parseTimeToCentis: trimming', () => {
  assert.equal(parseTimeToCentis('  3:40  '), 22000);
  assert.equal(parseTimeToCentis('  40  '), 4000);
});

test('parseTimeToCentis: invalid', () => {
  assert.equal(parseTimeToCentis('3:'), undefined);
  assert.equal(parseTimeToCentis(':40'), undefined);
  assert.equal(parseTimeToCentis('3::40'), undefined);
  assert.equal(parseTimeToCentis('1:2:3:4'), undefined);
  assert.equal(parseTimeToCentis('abc'), undefined);
  assert.equal(parseTimeToCentis('3:40:30:00'), undefined);
  assert.equal(parseTimeToCentis('-5'), undefined);
  assert.equal(parseTimeToCentis('3:-40'), undefined);
});

test('parseTimeToCentis: reject trailing garbage in segments', () => {
  assert.equal(parseTimeToCentis('5:13a'), undefined);
  assert.equal(parseTimeToCentis('5a:13'), undefined);
  assert.equal(parseTimeToCentis('3:40.2x'), undefined);
  assert.equal(parseTimeToCentis('1:30foo'), undefined);
  assert.equal(parseTimeToCentis('40.5'), 4050); // valid decimal, no garbage
});

test('parseTimeToCentis: segment bounds (min/sec 0-59)', () => {
  assert.equal(parseTimeToCentis('60'), undefined);
  assert.equal(parseTimeToCentis('90'), undefined);
  assert.equal(parseTimeToCentis('1:60'), undefined);
  assert.equal(parseTimeToCentis('0:90'), undefined);
  assert.equal(parseTimeToCentis('60:00'), undefined);
  assert.equal(parseTimeToCentis('1:30:60'), undefined);
  assert.equal(parseTimeToCentis('1:60:00'), undefined);
  assert.equal(parseTimeToCentis('1:59'), 11900); // 1*60+59 = 119s
  assert.equal(parseTimeToCentis('0:59'), 5900);
});

test('parseTimeToCentis: tenths and hundredths (PGN-style)', () => {
  assert.equal(parseTimeToCentis('40.5'), 4050);
  assert.equal(parseTimeToCentis('40.55'), 4055);
  assert.equal(parseTimeToCentis('40.12'), 4012);
  assert.equal(parseTimeToCentis('3.7'), 370);
  assert.equal(parseTimeToCentis('0.1'), 10);
  assert.equal(parseTimeToCentis('40.0'), 4000);
  assert.equal(parseTimeToCentis('3:40.2'), 22020);
  assert.equal(parseTimeToCentis('3:40.25'), 22025);
  assert.equal(parseTimeToCentis('40.'), undefined);
});

test('formatClockFromCentis: zero and negative', () => {
  assert.equal(formatClockFromCentis(0), '0:00:00');
  assert.equal(formatClockFromCentis(-100), '0:00:00');
});

test('formatClockFromCentis: seconds only', () => {
  assert.equal(formatClockFromCentis(4000), '40');
  assert.equal(formatClockFromCentis(4050), '40.5');
  assert.equal(formatClockFromCentis(4055), '40.55');
  assert.equal(formatClockFromCentis(370), '3.7');
});

test('formatClockFromCentis: minutes', () => {
  assert.equal(formatClockFromCentis(22000), '3:40');
  assert.equal(formatClockFromCentis(6000), '1:00');
  assert.equal(formatClockFromCentis(22020), '3:40.2');
  assert.equal(formatClockFromCentis(22025), '3:40.25');
});

test('formatClockFromCentis: hours', () => {
  assert.equal(formatClockFromCentis(382000), '1:03:40');
  assert.equal(formatClockFromCentis(360000), '1:00:00');
  assert.equal(formatClockFromCentis(3600000), '10:00:00');
});
