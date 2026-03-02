import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

describe('roundToCurrency', async () => {
  const { roundToCurrency } = await import('../../lib/src/i18n');

  await test('USD', async () => {
    document.documentElement.lang = 'en-US';
    const currency = 'USD';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1.01);
    assert.equal(roundToCurrency(1.01, currency), 1.01);
    assert.equal(roundToCurrency(9.99, currency), 9.99);
  });

  await test('EUR', async () => {
    document.documentElement.lang = 'fr-FR';
    const currency = 'EUR';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1.01);
    assert.equal(roundToCurrency(1.01, currency), 1.01);
    assert.equal(roundToCurrency(9.99, currency), 9.99);
  });

  await test('JPY - a currency that does not use decimals', async () => {
    document.documentElement.lang = 'ja-JP';
    const currency = 'JPY';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1);
    assert.equal(roundToCurrency(1.01, currency), 1);
    assert.equal(roundToCurrency(9.99, currency), 10);
  });
});

describe('dateParsing', async () => {
  process.env.TZ = 'UTC';
  document.documentElement.lang = 'en-US';
  const { toDate } = await import('../../lib/src/i18n');

  await test('string: day only', () => {
    assert.strictEqual(toDate('2024-01-01').toISOString(), '2024-01-01T00:00:00.000Z');
  });

  await test('string: iso full', () => {
    assert.strictEqual(toDate('2026-03-01T07:59:00.161611Z').toISOString(), '2026-03-01T07:59:00.161Z');
  });

  await test('string: utc', () => {
    assert.strictEqual(toDate('Tue, 17 April 2024 23:50:21 GMT').toISOString(), '2024-04-17T23:50:21.000Z');
  });

  await test('string: epoch timestamp', () => {
    assert.strictEqual(toDate('1772357604952').toISOString(), '2026-03-01T09:33:24.952Z');
  });

  await test('string: date toString', () => {
    assert.strictEqual(
      toDate('Tue May 12 2020 18:50:21 GMT-0500 (Central Daylight Time)').toISOString(),
      '2020-05-12T23:50:21.000Z',
    );
  });

  await test('number: epoch timestamp', () => {
    assert.strictEqual(toDate(1772355485000).toISOString(), '2026-03-01T08:58:05.000Z');
  });

  await test('date', () => {
    assert.strictEqual(toDate(new Date('2/1/22')).toISOString(), '2022-02-01T00:00:00.000Z');
  });

  await test('invalid date: random string', () => {
    assert.throws(() => toDate('abc123').toISOString());
  });

  await test('invalid date: infinity', () => {
    assert.throws(() => toDate(Number.POSITIVE_INFINITY).toISOString());
  });

  await test('invalid date: NaN', () => {
    assert.throws(() => toDate(Number.NaN).toISOString());
  });
});
