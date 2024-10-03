import { Mode, Sort } from './interfaces';
import { storage } from 'common/storage';

interface Store<A> {
  set(v: string): A;
  get(): A;
}

export interface Stores {
  mode: Store<Mode>;
  sort: Store<Sort>;
}

interface Config<A> {
  key: string;
  fix(v: string | null): A;
}

const mode: Config<Mode> = {
  key: 'lobby.mode',
  fix: (m: string | null) => (m ? (m as Mode) : 'list'),
};
const sort: Config<Sort> = {
  key: 'lobby.sort',
  fix: (s: string | null) => (s ? (s as Sort) : 'rating'),
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
    mode: makeStore<Mode>(mode, userId),
    sort: makeStore<Sort>(sort, userId),
  };
}
