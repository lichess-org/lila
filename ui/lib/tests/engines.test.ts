import assert from 'node:assert/strict';
import { before, describe, mock, test } from 'node:test';

import type { CevalCtrl } from '../src/ceval/ctrl';
import type { Engines } from '../src/ceval/engines/engines';

const enginesUrl = new URL('../src/ceval/engines/engines.ts', import.meta.url).href;
const permalogUrl = new URL('../src/permalog.ts', import.meta.url).href;

describe('external engine deletion', () => {
  let makeEngines: (externalEngines: { id: string; name: string }[]) => Engines;

  before(async () => {
    mock.module(permalogUrl, { namedExports: { log: () => Promise.resolve() } });
    mock.method(globalThis, 'fetch', async () => new Response('{"ok":true}', { status: 200 }));
    const { Engines } = await import(enginesUrl);
    makeEngines = externalEngines => {
      const ctrl = {
        rules: 'chess',
        nonStandardMaterial: false,
        opts: { externalEngines },
      } as unknown as CevalCtrl;
      return new Engines(ctrl);
    };
  });

  const external = (id: string, name: string) => ({
    id,
    name,
    maxThreads: 8,
    maxHash: 512,
    variants: ['chess'],
    clientSecret: 'ees_test',
    endpoint: 'http://localhost:8666',
  });

  test('deleting the active engine deactivates it', async () => {
    const engines = makeEngines([external('eei_a', 'Engine A'), external('eei_b', 'Engine B')]);
    engines.setActive('eei_b');
    assert.equal(engines.external()?.id, 'eei_b');

    assert.equal(await engines.deleteExternal('eei_b'), true);
    assert.deepEqual(
      engines.externalEngines.map(e => e.id),
      ['eei_a'],
    );
    assert.notEqual(engines.active()?.id, 'eei_b');
    assert.notEqual(engines.external()?.id, 'eei_b');
    assert.equal(
      engines.supporting({ rules: 'chess' }).some(e => e.id === 'eei_b'),
      false,
    );
  });

  test('the last external engine falls back to a browser engine', async () => {
    const engines = makeEngines([external('eei_a', 'Engine A')]);
    engines.setActive('eei_a');

    assert.equal(await engines.deleteExternal('eei_a'), true);
    assert.equal(engines.external(), undefined);
    assert.equal(engines.active()?.tech !== 'EXTERNAL', true);
  });

  test('deleting an inactive engine keeps the active one', async () => {
    const engines = makeEngines([external('eei_a', 'Engine A'), external('eei_b', 'Engine B')]);
    engines.setActive('eei_b');

    assert.equal(await engines.deleteExternal('eei_a'), true);
    assert.equal(engines.active()?.id, 'eei_b');
    assert.equal(await engines.deleteExternal('eei_a'), false);
  });
});
