import { objectStorage, type ObjectStorage, type DbInfo } from './objectStorage';

export interface PermaLog {
  (...args: any[]): Promise<number | void>;
  clear(): Promise<void>;
  get(): Promise<string>;
}

export const log: PermaLog = makeLog(
  {
    db: 'log--db',
    store: 'log',
    version: 3,
    upgrade: (_: any, store: IDBObjectStore) => store?.clear(), // blow it all away when we rev version
  },
  parseInt(localStorage.getItem('log.window') || '100'),
);

export function makeLog(dbInfo: DbInfo, windowSize: number): PermaLog {
  let store: ObjectStorage<string, number>;
  let resolveReady: () => void;
  let lastKey = 0;
  let drift = 0.001;

  const ready = new Promise<void>(resolve => (resolveReady = resolve));

  (Error.prototype as any).toJSON ??= function () {
    return { [this.name]: this.message, stack: this.stack };
  };

  objectStorage<string, number>(dbInfo)
    .then(async s => {
      store = s;
      resolveReady();
    })
    .catch(e => {
      console.error(e);
      window.indexedDB.deleteDatabase(dbInfo.db ?? dbInfo.store);
      resolveReady();
    });

  function stringify(val: any): string {
    return !val || typeof val === 'string' ? String(val) : JSON.stringify(val);
  }

  const log: PermaLog = (...args: any[]) => {
    if (dbInfo.store === 'log') console.log(...args);
    const msg =
      (dbInfo.store === 'log' && site.info ? `#${site.info.commit.substring(0, 7)} - ` : '') +
      args.map(stringify).join(' ');
    let nextKey = Date.now();
    if (nextKey === lastKey) {
      nextKey += drift;
      drift += 0.001;
    } else {
      drift = 0.001;
      lastKey = nextKey;
    }
    return ready.then(() => store?.put(nextKey, msg)).catch(console.error);
  };

  log.clear = async () => {
    await ready;
    await store?.clear();
    lastKey = 0;
  };

  log.get = async (): Promise<string> => {
    await ready;
    if (!store) return '';
    try {
      const keys = await store.list();
      if (windowSize >= 0 && keys.length > windowSize) {
        await store.remove(IDBKeyRange.upperBound(keys[keys.length - windowSize], true));
      }
    } catch (e) {
      console.error(e);
      store.clear();
      window.indexedDB.deleteDatabase(dbInfo.db ?? dbInfo.store);
      return '';
    }
    const [keys, vals] = await Promise.all([store.list(), store.getMany()]);
    return keys.map((k, i) => `${new Date(k).toISOString().replace(/[TZ]/g, ' ')}${vals[i]}`).join('\n');
  };

  return log;
}
