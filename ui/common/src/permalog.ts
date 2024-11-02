import { objectStorage, ObjectStorage, DbInfo } from './objectStorage';

export const log: LichessLog = makeLog();

interface LichessLog {
  (...args: any[]): Promise<number | void>;
  clear(): Promise<void>;
  get(): Promise<string>;
}

function makeLog(): LichessLog {
  const dbInfo: DbInfo = {
    db: 'log--db',
    store: 'log',
    version: 3,
    upgrade: (_: any, store: IDBObjectStore) => store?.clear(), // blow it all away when we rev version
  };
  const defaultLogWindow = 100;

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
      try {
        const keys = await s.list();
        const window = parseInt(localStorage.getItem('log.window') ?? `${defaultLogWindow}`);
        const constrained = window >= 0 && window <= 10000 ? window : defaultLogWindow;
        if (keys.length > constrained) {
          await s.remove(IDBKeyRange.upperBound(keys[keys.length - constrained], true));
        }
        store = s;
      } catch (e) {
        console.error(e);
        s.clear();
      }
      resolveReady();
    })
    .catch(e => {
      console.error(e);
      window.indexedDB.deleteDatabase(dbInfo.db!);
      resolveReady();
    });

  function stringify(val: any): string {
    return !val || typeof val === 'string' ? String(val) : JSON.stringify(val);
  }

  const log: LichessLog = (...args: any[]) => {
    console.log(...args);
    const msg = `#${site.info ? `${site.info.commit.substring(0, 7)} - ` : ''}${args
      .map(stringify)
      .join(' ')}`;
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
    const [keys, vals] = await Promise.all([store.list(), store.getMany()]);
    return keys.map((k, i) => `${new Date(k).toISOString().replace(/[TZ]/g, ' ')}${vals[i]}`).join('\n');
  };

  return log;
}
