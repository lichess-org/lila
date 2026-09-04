import assert from 'node:assert/strict';
import { afterEach, describe, mock, test } from 'node:test';

import { CevalCtrl } from '../src/ceval/ctrl';
import type { ExternalEngineInfoFromServer } from '../src/ceval/types';

const variant: Variant = { key: 'standard', name: 'Standard', short: 'Std' };

const ext = (id: string): ExternalEngineInfoFromServer => ({
  id,
  name: id,
  variants: ['chess'],
  maxHash: 16,
  maxThreads: 1,
  clientSecret: 's',
  endpoint: 'http://engine.test',
});

const makeCtrl = () =>
  new CevalCtrl({
    variant,
    emit: () => {},
    onUciHover: () => {},
    redraw: () => {},
    externalEngines: [ext('ext-a'), ext('ext-b')],
  });

describe('deleteExternal', () => {
  afterEach(() => mock.restoreAll());

  test('deletes the requested engine, not a previously selected one', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    const ceval = makeCtrl();
    ceval.selectEngine('ext-a');
    ceval.selectEngine('ext-b');

    assert.equal(await ceval.deleteExternal('ext-b'), true);
    assert.equal(fetchMock.mock.calls[0].arguments[0], '/api/external-engine/ext-b');
    assert.deepEqual(
      ceval.engines.externalEngines.map(e => e.id),
      ['ext-a'],
    );
    assert.notEqual(ceval.engines.active()?.id, 'ext-b');
    assert.equal(ceval.storedEngine(), ceval.engines.active()?.id);
  });

  test('keeps the current engine when deleting another', async () => {
    mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    const ceval = makeCtrl();
    ceval.selectEngine('ext-b');

    assert.equal(await ceval.deleteExternal('ext-a'), true);
    assert.equal(ceval.engines.active()?.id, 'ext-b');
    assert.equal(ceval.storedEngine(), 'ext-b');
    assert.deepEqual(
      ceval.engines.externalEngines.map(e => e.id),
      ['ext-b'],
    );
  });

  test('does not mutate state when the api fails', async () => {
    mock.method(globalThis, 'fetch', async () => new Response(null, { status: 500 }));
    const ceval = makeCtrl();
    ceval.selectEngine('ext-a');

    assert.equal(await ceval.deleteExternal('ext-a'), false);
    assert.equal(ceval.engines.active()?.id, 'ext-a');
    assert.equal(ceval.engines.externalEngines.length, 2);
  });

  test('returns false for an unknown engine without calling the api', async () => {
    const fetchMock = mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
    const ceval = makeCtrl();

    assert.equal(await ceval.deleteExternal('missing'), false);
    assert.equal(fetchMock.mock.callCount(), 0);
  });
});
