import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { isVersionCompatible } from '../src/device';

describe('test isVersionCompatible', () => {
  test('isVersionCompatible', () => {
    assert.equal(isVersionCompatible('15b1', { atLeast: '1.1.1', below: '15.0.1' }), true);
    assert.equal(isVersionCompatible('10.2', { atLeast: '10.10' }), false);
    assert.equal(isVersionCompatible('15.4.2', { atLeast: '15', below: '15.4.3' }), true);
    assert.equal(isVersionCompatible('15.4.2', { atLeast: '14.99.99', below: '15.4.2' }), false);
    assert.equal(isVersionCompatible('15', { below: '15' }), false);
    assert.equal(isVersionCompatible('15', { below: '15.0' }), false);
    assert.equal(isVersionCompatible('15', { below: '15.1' }), true);
    assert.equal(isVersionCompatible('15.1.0', { below: '15.1' }), false);
    assert.equal(isVersionCompatible('15.1', { atLeast: '15.1', below: '15.1' }), false);
    assert.equal(isVersionCompatible('122_2a7', { below: '122.2' }), false);
    assert.equal(isVersionCompatible('122_2a7', { atLeast: '122.2' }), true);
    assert.equal(isVersionCompatible('132.0.0.0', { below: '132' }), false);
    assert.equal(isVersionCompatible('132.0.0.0', { atLeast: '132' }), true);
    assert.equal(isVersionCompatible('5', {}), true);
    assert.equal(isVersionCompatible('1'), true);
    assert.equal(isVersionCompatible('', { below: '6' }), false);
    assert.equal(isVersionCompatible('', {}), false);
    assert.equal(isVersionCompatible(false, {}), false);
  });
});
