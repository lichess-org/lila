import assert from 'node:assert/strict';
import { afterEach, beforeEach, describe, mock, test } from 'node:test';

import { ExternalEngine } from '../src/ceval/engines/externalEngine';
import type { ExternalEngineInfo, Work } from '../src/ceval/types';

const info: ExternalEngineInfo = {
  id: 'eei_test',
  name: 'Test engine',
  tech: 'EXTERNAL',
  preferred: true,
  minThreads: 1,
  maxThreads: 4,
  maxHash: 16,
  variants: ['chess'],
  clientSecret: 'ces_test',
  endpoint: 'https://engine.example',
};

const workAfter = (moves: string[]): Work => ({
  path: '',
  ply: moves.length,
  threatMode: false,
  variant: 'chess',
  threads: 1,
  stopRequested: false,
  search: { movetime: 1000 },
  multiPv: 1,
  initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
  currentFen: '',
  moves,
  emit: () => {},
});

interface Sent {
  moves: string[];
  signal: AbortSignal;
}

describe('ExternalEngine request timing', () => {
  let now = 0;
  let sent: Sent[] = [];
  let engine: ExternalEngine;

  // setImmediate is not mocked, so this lets promise callbacks run after a tick
  const settle = () => new Promise(resolve => setImmediate(resolve));
  const advance = async (ms: number) => {
    now += ms;
    mock.timers.tick(ms);
    await settle();
  };
  const step = async (moves: string[]) => {
    engine.start(workAfter(moves));
    await settle();
  };
  const sentMoves = () => sent.map(s => s.moves);

  beforeEach(() => {
    now = 10_000;
    sent = [];
    mock.timers.enable({ apis: ['setTimeout'] });
    mock.method(performance, 'now', () => now);
    mock.method(globalThis, 'fetch', async (_url: string, init: RequestInit) => {
      const body = JSON.parse(String(init.body));
      sent.push({ moves: body.work.moves, signal: init.signal! });
      return new Response('');
    });
    engine = new ExternalEngine(info, () => {});
  });

  afterEach(() => {
    engine.destroy();
    mock.timers.reset();
    mock.restoreAll();
  });

  test('requests a position at once after a pause', async () => {
    await step(['e2e4']);
    assert.deepEqual(sentMoves(), [['e2e4']]);

    await advance(1000);
    await step(['e2e4', 'e7e5']);
    assert.deepEqual(sentMoves(), [['e2e4'], ['e2e4', 'e7e5']]);
  });

  test('stepping quickly requests only the position the user stops on', async () => {
    await step(['e2e4']);
    await advance(100);
    await step(['e2e4', 'e7e5']);
    await advance(100);
    await step(['e2e4', 'e7e5', 'g1f3']);
    assert.deepEqual(sentMoves(), [['e2e4']], 'nothing goes out while still stepping');

    await advance(250);
    assert.deepEqual(sentMoves(), [['e2e4'], ['e2e4', 'e7e5', 'g1f3']]);
  });

  test('holding down an arrow key requests nothing more until it is released', async () => {
    const moves: string[] = [];
    for (let i = 0; i < 30; i++) {
      moves.push(`m${i}`);
      await step([...moves]);
      await advance(33);
    }
    assert.equal(sent.length, 1, 'only the first position went out while the key was held');

    await advance(250);
    assert.equal(sent.length, 2);
    assert.deepEqual(sent[1].moves, moves);
  });

  test('aborts the earlier request when the position changes', async () => {
    await step(['e2e4']);
    await step(['e2e4', 'e7e5']);
    assert.equal(sent[0].signal.aborted, true);
  });
});
