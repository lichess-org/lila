import type { Tab, Mode, Sort, PoolMode } from './interfaces';
import { storage } from 'lib/storage';

interface Store<A> {
  set(v: string): A;
  get(): A;
}

export interface Stores {
  tab: Store<Tab>;
  mode: Store<Mode>;
  sort: Store<Sort>;
  poolMode: Store<PoolMode>;
}

interface Config<A> {
  key: string;
  fix(v: string | null): A;
}

const tab: Config<Tab> = {
  key: 'lobby.tab',
  fix(t: string | null): Tab {
    if (<Tab>t) return t as Tab;
    return 'pools';
  },
};
const mode: Config<Mode> = {
  key: 'lobby.mode',
  fix(m: string | null): Mode {
    if (<Mode>m) return m as Mode;
    return 'list';
  },
};
const sort: Config<Sort> = {
  key: 'lobby.sort',
  fix(s: string | null): Sort {
    if (<Sort>s) return s as Sort;
    return 'rating';
  },
};
const poolMode: Config<PoolMode> = {
  key: 'lobby.poolMode',
  fix(m: string | null): PoolMode {
    if (<PoolMode>m) return m as PoolMode;
    return 'quick_pairing';
  },
};

function makeStore<A>(conf: Config<A>, userId?: string): Store<A> {
  const fullKey = conf.key + ':' + (userId || '-');
  return {
    set(v: string): A {
      const t: A = conf.fix(v);
      storage.set(fullKey, '' + t);
      return t;
    },
    get(): A {
      return conf.fix(storage.get(fullKey));
    },
  };
}

export function make(userId?: string): Stores {
  return {
    tab: makeStore<Tab>(tab, userId),
    mode: makeStore<Mode>(mode, userId),
    poolMode: makeStore<PoolMode>(poolMode, userId),
    sort: makeStore<Sort>(sort, userId),
  };
}
