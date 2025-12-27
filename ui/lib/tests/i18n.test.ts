import { describe, test } from 'node:test';
import assert from 'node:assert/strict';

describe('roundToCurrency', () => {
  test('USD', async () => {
    document.documentElement.lang = 'en-US';
    const { roundToCurrency } = await import('../../lib/src/i18n');
    const currency = 'USD';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1.01);
    assert.equal(roundToCurrency(1.01, currency), 1.01);
    assert.equal(roundToCurrency(9.99, currency), 9.99);
  });

  test('EUR', async () => {
    document.documentElement.lang = 'fr-FR';
    const { roundToCurrency } = await import('../../lib/src/i18n');
    const currency = 'EUR';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1.01);
    assert.equal(roundToCurrency(1.01, currency), 1.01);
    assert.equal(roundToCurrency(9.99, currency), 9.99);
  });

  test('JPY - a currency that does not use decimals', async () => {
    document.documentElement.lang = 'ja-JP';
    const { roundToCurrency } = await import('../../lib/src/i18n');
    const currency = 'JPY';
    assert.equal(roundToCurrency(1.0, currency), 1);
    assert.equal(roundToCurrency(1.005, currency), 1);
    assert.equal(roundToCurrency(1.01, currency), 1);
    assert.equal(roundToCurrency(9.99, currency), 10);
  });
});
