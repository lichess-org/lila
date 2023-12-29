import { objectStorage, ObjectStorage } from 'common/objectStorage';

export default function makeLog(): LichessLog {
  let store: ObjectStorage<string, number>;
  let resolveReady: () => void;
  let lastKey = 0;
  let drift = 0.001;

  const keep = 1000; // trimmed on startup
  const ready = new Promise<void>(resolve => (resolveReady = resolve));

  objectStorage<string, number>({ store: 'log' })
    .then(async s => {
      const keys = await s.list();
      if (keys.length > keep) {
        await s.remove(IDBKeyRange.upperBound(keys[keys.length - keep], true));
      }
      store = s;
      resolveReady();
    })
    .catch(() => {
      resolveReady();
      objectStorage<string, number>({ store: 'log' })
        .then(s => s.clear())
        .catch(() => {});
    });

  function stringify(val: any): string {
    return !val || typeof val === 'string' ? String(val) : JSON.stringify(val);
  }

  const log: any = async (...args: any[]) => {
    const msg = `${lichess.info.commit.substr(0, 7)} - ${args.map(stringify).join(' ')}`;
    let nextKey = Date.now();
    console.log(...args);
    if (nextKey === lastKey) {
      nextKey += drift;
      drift += 0.001;
    } else {
      drift = 0.001;
      lastKey = nextKey;
    }
    await ready;
    await store?.put(nextKey, msg);
  };

  log.clear = async () => {
    await ready;
    await store?.clear();
    lastKey = 0;
  };

  log.get = async (): Promise<string> => {
    await ready;
    const [keys, vals] = await Promise.all([store.list(), store.getMany()]);
    return keys.map((k, i) => `${new Date(k).toISOString()} ${vals[i]}`).join('\n');
  };

  window.addEventListener('error', async e => {
    log(
      `${window.location.href} - ${e.message} (${e.filename}:${e.lineno}:${e.colno})\n${
        e.error?.stack ?? ''
      }`.trim(),
    );
  });
  window.addEventListener('unhandledrejection', async e => {
    log(`${window.location.href} - ${e.reason}\n${e.reason.stack ?? ''}`.trim());
  });

  return log;
}
