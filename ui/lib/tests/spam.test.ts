import { test, mock, before } from 'node:test';
import assert from 'node:assert/strict';

const spamUrl = new URL('../src/chat/spam.ts', import.meta.url).href;
const xhrUrl = new URL('../src/xhr.ts', import.meta.url).href;

test('self report', async () => {
  const textMock = mock.fn(async (url: string) => {
    assert.equal(url, '/jslog/lichess.org/?n=spam');
    return 'ok';
  });

  let selfReport: (msg: string) => Promise<unknown>;

  before(async () => {
    const xhr = await import(xhrUrl);
    mock.module(xhrUrl, { namedExports: { ...xhr, text: textMock } });

    ({ selfReport } = await import(spamUrl));
  });

  test('self report', async () => {
    await selfReport('check out bit.ly/spam');
    assert.equal(textMock.mock.callCount(), 1);
  });
});
