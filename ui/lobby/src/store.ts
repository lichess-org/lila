import { Tab, Mode, Sort } from './interfaces';

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
  fix(v: string): A;
}

const tab: Config<Tab> = {
  key: 'lobby.tab',
  fix(t: string): Tab {
    if (<Tab>t) return t as Tab;
    return 'pools';
  }
};
const mode: Config<Mode> = {
  key: 'lobby.mode',
  fix(m: string): Mode {
    if (<Mode>m) return m as Mode;
    return 'list';
  }
};
const sort: Config<Sort> = {
  key: 'lobby.sort',
  fix(s: string): Sort {
    if (<Sort>s) return s as Sort;
    return 'rating';
  }
};

function makeStore<A>(conf: Config<A>, userId: string): Store<A> {
  const fullKey = conf.key + ':' + (userId || '-');
  return {
    set(v: string): A {
      const t: A = conf.fix(v);
      window.lidraughts.storage.set(fullKey, '' + t as string);
      return t;
    },
    get(): A {
      return conf.fix(window.lidraughts.storage.get(fullKey));
    }
  };
}

export function make(userId: string): Stores {
  return {
    tab: makeStore<Tab>(tab, userId),
    mode: makeStore<Mode>(mode, userId),
    sort: makeStore<Sort>(sort, userId)
  }
};
