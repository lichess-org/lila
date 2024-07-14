import { objectStorage, ObjectStorage, DbInfo } from 'common/objectStorage';
import { alert } from 'common/dialog';

const dbInfo: DbInfo = {
  db: 'log--db',
  store: 'log',
  version: 2,
  upgrade: (_: any, store: IDBObjectStore) => store?.clear(), // blow it all away when we rev version
};

const defaultLogWindow = 100;

export default function makeLog(): LichessLog {
  let store: ObjectStorage<string, number>;
  let resolveReady: () => void;
  let lastKey = 0;
  let drift = 0.001;

  const ready = new Promise<void>(resolve => (resolveReady = resolve));

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

  const log: LichessLog = async (...args: any[]) => {
    const msg = `#${site.info ? `${site.info.commit.substring(0, 7)} - ` : ''}${args
      .map(stringify)
      .join(' ')}`;
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
    if (!store) return '';
    const [keys, vals] = await Promise.all([store.list(), store.getMany()]);
    return keys.map((k, i) => `${new Date(k).toISOString().replace(/[TZ]/g, ' ')}${vals[i]}`).join('\n');
  };

  function terseHref(): string {
    return window.location.href.replace(/^(https:\/\/)?(?:lichess|lichess1)\.org\//, '/');
  }

  window.addEventListener('error', async e => {
    const loc = e.filename ? ` - (${e.filename}:${e.lineno}:${e.colno})` : '';
    log(`${terseHref()} - ${e.message}${loc}\n${e.error?.stack ?? ''}`.trim());
    if (site.debug) alert(`${e.message}${loc}\n${e.error?.stack ?? ''}`);
  });
  window.addEventListener('unhandledrejection', async e => {
    log(`${terseHref()} - ${e.reason}`);
    if (site.debug) alert(`${e.reason}`);
  });

  return log;
}
