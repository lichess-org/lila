import assert from 'node:assert/strict';
import { before, describe, mock, test } from 'node:test';

import type { CevalCtrl } from '../src/ceval/ctrl';
import type { ClientEval } from '../src/tree/types';

const ctrlUrl = new URL('../src/ceval/ctrl.ts', import.meta.url).href;
const permalogUrl = new URL('../src/permalog.ts', import.meta.url).href;

const external = {
  id: 'eei_a',
  name: 'Engine A',
  maxThreads: 8,
  maxHash: 512,
  variants: ['chess'],
  clientSecret: 'ees_test',
  endpoint: 'http://localhost:8666',
};

const clientEval = (over: Partial<ClientEval>): ClientEval => ({
  fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
  depth: 20,
  nodes: 1_000_000,
  pvs: [{ moves: ['e2e4'], cp: 30 }],
  ...over,
});
const cloud = clientEval({ cloud: true, depth: 40, nodes: 90_000_000 });

describe('cloud evals with an external engine', () => {
  let ceval: CevalCtrl;

  before(async () => {
    mock.module(permalogUrl, { namedExports: { log: () => Promise.resolve() } });
    const { CevalCtrl } = await import(ctrlUrl);
    ceval = new CevalCtrl({
      variant: { key: 'standard', name: 'Standard', short: 'Std' },
      emit: () => {},
      onUciHover: () => {},
      redraw: () => {},
      externalEngines: [external],
    });
  });

  test('a cloud eval never replaces the external engine', () => {
    ceval.engines.setActive('eei_a');
    assert.equal(ceval.engines.external()?.id, 'eei_a');
    assert.equal(ceval.preferLatestEval(cloud, undefined), false);
    assert.equal(ceval.preferLatestEval(cloud, clientEval({ engineId: 'eei_a' })), false);
  });

  test('the external engine still deepens its own eval', () => {
    ceval.engines.setActive('eei_a');
    const deeper = clientEval({ depth: 22, nodes: 2_000_000, engineId: 'eei_a' });
    assert.equal(ceval.preferLatestEval(deeper, clientEval({ engineId: 'eei_a' })), true);
  });

  test('a browser engine keeps taking cloud evals', () => {
    const browser = ceval.engines.supporting({ rules: 'chess', filter: 'browser' })[0];
    ceval.engines.setActive(browser.id);
    assert.equal(ceval.engines.external(), undefined);
    assert.equal(ceval.preferLatestEval(cloud, undefined), true);
    assert.equal(ceval.preferLatestEval(cloud, clientEval({ engineId: browser.id })), true);
  });
});
