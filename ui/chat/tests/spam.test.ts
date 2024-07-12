import { expect, test, vi } from 'vitest';
import { selfReport } from '../src/spam';
import * as xhr from 'common/xhr';

test('self report', () => {
  vi.stubGlobal('window', { location: { href: 'https://lichess.org/abcdef123456' } });

  vi.stubGlobal('site', {
    storage: {
      set: (key: string, value: string) => {
        expect(key).toBe('chat-spam');
        expect(value).toBe('1');
      },
      get: () => '0',
    },
  });

  const spy = vi.spyOn(xhr, 'text').mockImplementation((url: string) => {
    expect(url).toBe('/jslog/abcdef123456?n=spam');
    return Promise.resolve('ok');
  });

  selfReport('check out bit.ly/spam');
  expect(spy).toHaveBeenCalled();
});
