// no side effects allowed due to re-export by index.ts

import { defined, notNull, type Prop, withEffect } from './common';

export const storage: LichessStorageHelper = builder(window.localStorage);
export const tempStorage: LichessStorageHelper = builder(window.sessionStorage);

export interface StoredProp<V> extends Prop<V> {
  (replacement?: V): V;
}

export function storedProp<V>(
  key: string,
  defaultValue: V,
  fromStr: (str: string) => V,
  toStr: (v: V) => string,
): StoredProp<V> {
  const compatKey = 'analyse.' + key;
  let cached: V;
  return function (replacement?: V) {
    if (defined(replacement) && replacement != cached) {
      cached = replacement;
      storage.set(key, toStr(replacement));
    } else if (!defined(cached)) {
      const compatValue = storage.get(compatKey);
      if (notNull(compatValue)) {
        storage.set(key, compatValue);
        storage.remove(compatKey);
      }
      const str = storage.get(key);
      cached = str === null ? defaultValue : fromStr(str);
    }
    return cached;
  };
}

export const storedStringProp = (k: string, defaultValue: string): StoredProp<string> =>
  storedProp<string>(
    k,
    defaultValue,
    str => str,
    v => v,
  );

export const storedBooleanProp = (k: string, defaultValue: boolean): StoredProp<boolean> =>
  storedProp<boolean>(
    k,
    defaultValue,
    str => str === 'true',
    v => v.toString(),
  );

export const storedStringPropWithEffect = (
  k: string,
  defaultValue: string,
  effect: (v: string) => void,
): Prop<string> => withEffect(storedStringProp(k, defaultValue), effect);

export const storedBooleanPropWithEffect = (
  k: string,
  defaultValue: boolean,
  effect: (v: boolean) => void,
): Prop<boolean> => withEffect(storedBooleanProp(k, defaultValue), effect);

export const storedIntProp = (k: string, defaultValue: number): StoredProp<number> =>
  storedProp<number>(
    k,
    defaultValue,
    str => Number(str),
    v => v + '',
  );

export const storedIntPropWithEffect = (
  k: string,
  defaultValue: number,
  effect: (v: number) => void,
): Prop<number> => withEffect(storedIntProp(k, defaultValue), effect);

export const storedJsonProp =
  <V>(key: string, defaultValue: () => V): Prop<V> =>
  (v?: V) => {
    if (defined(v)) {
      storage.set(key, JSON.stringify(v));
      return v;
    }
    const ret = JSON.parse(storage.get(key)!);
    return ret !== null ? ret : defaultValue();
  };

export interface StoredMap<V> {
  (key: string): V;
  (key: string, value: V): void;
}

export const storedMap = <V>(propKey: string, maxSize: number, defaultValue: () => V): StoredMap<V> => {
  const prop = storedJsonProp<[string, V][]>(propKey, () => []);
  const map = new Map<string, V>(prop());
  return (key: string, v?: V) => {
    if (defined(v)) {
      map.delete(key); // update insertion order as old entries are culled
      map.set(key, v);
      prop(Array.from(map.entries()).slice(-maxSize));
    }
    const ret = map.get(key);
    return defined(ret) ? ret : defaultValue();
  };
};

export const asProp =
  <V>(map: StoredMap<V>, key: string): Prop<V> =>
  (v?: V) => {
    if (defined(v)) {
      map(key, v);
      return v;
    }
    return map(key);
  };

export const storedMapAsProp = <V>(
  propKey: string,
  key: string,
  maxSize: number,
  defaultValue: () => V,
): Prop<V> => asProp(storedMap(propKey, maxSize, defaultValue), key);

export interface StoredSet<V> {
  (): Set<V>;
  (value: V): Set<V>;
}

export const storedSet = <V>(propKey: string, maxSize: number): StoredSet<V> => {
  const prop = storedJsonProp<V[]>(propKey, () => []);
  let set = new Set<V>(prop());
  return (v?: V) => {
    if (defined(v)) {
      set.add(v);
      set = new Set([...set].slice(-maxSize)); // sets maintain insertion order
      prop([...set]);
    }
    return set;
  };
};

export function once(key: string, every?: { seconds?: number; hours?: number; days?: number }): boolean {
  const now = Date.now();
  const last = Number(storage.get(key)) || 0;
  const seconds = (every?.seconds ?? 0) + (every?.hours ?? 0) * 3600 + (every?.days ?? 0) * 24 * 3600;

  if (last && (!every || now - last < seconds * 1000)) return false;
  storage.set(key, now.toString());
  return true;
}

export interface LichessStorage {
  get(): string | null;
  set(v: any): void;
  remove(): void;
  listen(f: (e: LichessStorageEvent) => void): void;
  fire(v?: string): void;
}

export interface LichessBooleanStorage {
  get(): boolean;
  getOrDefault(defaultValue: boolean): boolean;
  set(v: boolean): void;
  toggle(): void;
}

export interface LichessStorageHelper {
  make(k: string, ttl?: number): LichessStorage;
  boolean(k: string): LichessBooleanStorage;
  get(k: string): string | null;
  set(k: string, v: string): void;
  fire(k: string, v?: string): void;
  remove(k: string): void;
}

interface LichessStorageEvent {
  sri: string;
  nonce: number;
  value?: string;
}

function builder(storage: Storage): LichessStorageHelper {
  const api = {
    get: (k: string): string | null => storage.getItem(k),
    set: (k: string, v: string): void => storage.setItem(k, v),
    fire: (k: string, v?: string) =>
      storage.setItem(
        k,
        JSON.stringify({
          sri: site.sri,
          nonce: Math.random(), // ensure item changes
          value: v,
        }),
      ),
    remove: (k: string) => storage.removeItem(k),
    make: (k: string, ttl?: number) => {
      const bdKey = ttl && `${k}--bd`;
      const remove = () => {
        api.remove(k);
        if (bdKey) api.remove(bdKey);
      };
      return {
        get: () => {
          if (!bdKey) return api.get(k);
          const birthday = Number(api.get(bdKey));
          if (!birthday) api.set(bdKey, String(Date.now()));
          else if (Date.now() - birthday > ttl) remove();
          return api.get(k);
        },
        set: (v: any) => {
          api.set(k, v);
          if (bdKey) api.set(bdKey, String(Date.now()));
        },
        fire: (v?: string) => api.fire(k, v),
        remove,
        listen: (f: (e: LichessStorageEvent) => void) =>
          window.addEventListener('storage', e => {
            if (e.key !== k || e.storageArea !== storage || e.newValue === null) return;
            let parsed: LichessStorageEvent | null;
            try {
              parsed = JSON.parse(e.newValue);
            } catch (_) {
              return;
            }
            // check sri, because Safari fires events also in the original
            // document when there are multiple tabs
            if (parsed?.sri && parsed.sri !== site.sri) f(parsed);
          }),
      };
    },
    boolean: (k: string) => ({
      get: () => api.get(k) === '1',
      getOrDefault: (defaultValue: boolean) => {
        const stored = api.get(k);
        return stored === null ? defaultValue : stored === '1';
      },
      set: (v: boolean): void => api.set(k, v ? '1' : '0'),
      toggle: () => api.set(k, api.get(k) === '1' ? '0' : '1'),
    }),
  };
  return api;
}
