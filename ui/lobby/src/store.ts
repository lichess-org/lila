import { storage } from 'lib/storage';

import type { Tab, Mode, Sort } from './interfaces';

interface Store<A> {
  set(v: string): A;
  get(): A;
}

export interface Stores {
  tab: Store<Tab>;
  mode: Store<Mode>;
  sort: Store<Sort>;
}

interface Config<A> {
  key: string;
  fix(v: string | null): A;
}

function isTab(value: string | null): value is Tab {
  return value === 'pools' || value === 'real_time' || value === 'seeks' || value === 'now_playing';
}

function isMode(value: string | null): value is Mode {
  return value === 'list' || value === 'chart';
}

function isSort(value: string | null): value is Sort {
  return value === 'rating' || value === 'time';
}

const tab: Config<Tab> = {
  key: 'lobby.tab',
  fix(t: string | null): Tab {
    if (isTab(t)) return t;
    return 'pools';
  },
};
const mode: Config<Mode> = {
  key: 'lobby.mode',
  fix(m: string | null): Mode {
    if (isMode(m)) return m;
    return 'list';
  },
};
const sort: Config<Sort> = {
  key: 'lobby.sort',
  fix(s: string | null): Sort {
    if (isSort(s)) return s;
    return 'rating';
  },
};

function makeStore<A>(conf: Config<A>, userId?: string): Store<A> {
  const fullKey = conf.key + ':' + (userId || '-');
  return {
    set(v: string): A {
      const t: A = conf.fix(v);
      storage.set(fullKey, String(t));
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
    sort: makeStore<Sort>(sort, userId),
  };
}
