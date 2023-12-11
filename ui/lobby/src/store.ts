import { CustomGameTab, LobbyTab, Mode, Sort } from './interfaces';

interface Store<A> {
  set(v: string): A;
  get(): A;
}

export interface Stores {
  tab: Store<LobbyTab>;
  customGameTab: Store<CustomGameTab>;
  mode: Store<Mode>;
  sort: Store<Sort>;
}

interface Config<A> {
  key: string;
  fix(v: string | null): A;
}

const tab: Config<LobbyTab> = {
  key: 'lobby.tab',
  fix(t: string | null): LobbyTab {
    if (<LobbyTab>t) return t as LobbyTab; // doesn't fix, it amounts to a runtime null check
    return 'pools';
  },
};
const customGameTab: Config<CustomGameTab> = {
  key: 'customGame.tab',
  fix(t: string | null): CustomGameTab {
    if (<CustomGameTab>t) return t as CustomGameTab;
    return 'real_time';
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

function makeStore<A>(conf: Config<A>, userId?: string): Store<A> {
  const fullKey = conf.key + ':' + (userId || '-');
  return {
    set(v: string): A {
      const t: A = conf.fix(v);
      lichess.storage.set(fullKey, ('' + t) as string);
      return t;
    },
    get(): A {
      return conf.fix(lichess.storage.get(fullKey));
    },
  };
}

export function make(userId?: string): Stores {
  return {
    tab: makeStore<LobbyTab>(tab, userId),
    customGameTab: makeStore<CustomGameTab>(customGameTab, userId),
    mode: makeStore<Mode>(mode, userId),
    sort: makeStore<Sort>(sort, userId),
  };
}
