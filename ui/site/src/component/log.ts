import { objectStorage, ObjectStorage } from 'common/objectStorage';

/*

lichess.log('hello', {foo: 'bar'}, 52);     // log stuff
const everything = await lichess.log.get();  // get all log statements
lichess.log.clear();                         // clear idb store

*/

export function makeLog() {
  const keep = 1000; // recent log statements to keep (roughly), trimmed on startup

  let store: ObjectStorage<string>;
  let lastKey = Date.now();
  let resolveReady: () => void;
  const ready = new Promise<void>(resolve => (resolveReady = resolve));

  objectStorage<string>({ store: 'log' })
    .then(async s => {
      const keys = await s.list();
      if (keys.length > keep) {
        await Promise.all(keys.slice(0, keys.length - keep).map(k => s.remove(k)));
      }
      store = s;
      resolveReady();
    })
    .catch(() => resolveReady());

  async function* getLogs(batchSize = 100) {
    await ready;
    if (!store) return '';
    const keys = await store.list();
    for (let i = 0; i < keys.length; i += batchSize) {
      yield await Promise.all(keys.slice(i, i + batchSize).map(async k => [k, await store.get(k)]));
    }
  }

  function stringify(val: any): string {
    return !val || typeof val === 'string' ? String(val) : JSON.stringify(val);
  }

  let drift = 0.01;
  const log: any = async (...args: any[]) => {
    console.log(...args);
    await ready;
    let nextKey = Date.now();
    if (nextKey === lastKey) {
      nextKey += drift;
      drift += 0.01;
    } else {
      drift = 0.01;
      lastKey = nextKey;
    }
    const [intPart, fracPart] = String(nextKey).split('.');
    const key = `${intPart.padStart(16, '0')}${fracPart ? '.' + fracPart : ''}`;
    await store?.put(key, args.map(stringify).join(' '));
  };

  log.clear = async () => {
    await ready;
    await store?.clear();
    lastKey = 0;
  };

  log.get = async (): Promise<string> => {
    const logs = [];
    for await (const batch of getLogs()) {
      for (const log of batch) logs.push(`${new Date(parseInt(log[0])).toISOString()} - ${log[1]}`);
    }
    return logs.join('\n');
  };

  window.addEventListener('error', e => {
    lichess.log(
      `${e.message} (${e.filename}:${e.lineno}:${e.colno}) ${e.error?.stack ? `\n${e.error.stack}` : ''}`,
    );
  });

  window.addEventListener('unhandledrejection', e => {
    lichess.log(`${e.reason} ${e.reason.stack ? `${e.reason.stack}` : ''}`);
  });

  return log;
}
