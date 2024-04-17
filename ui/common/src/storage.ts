import { defined, notNull, Prop, Toggle, withEffect } from './common';

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
      site.storage.set(key, toStr(replacement));
    } else if (!defined(cached)) {
      const compatValue = site.storage.get(compatKey);
      if (notNull(compatValue)) {
        site.storage.set(key, compatValue);
        site.storage.remove(compatKey);
      }
      const str = site.storage.get(key);
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

export type StoredJsonProp<V> = Prop<V>;

export const storedJsonProp =
  <V>(key: string, defaultValue: () => V): StoredJsonProp<V> =>
  (v?: V) => {
    if (defined(v)) {
      site.storage.set(key, JSON.stringify(v));
      return v;
    }
    const ret = JSON.parse(site.storage.get(key)!);
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

export interface ToggleWithUsed extends Toggle {
  used: () => boolean;
}

export const toggleWithUsed = (key: string, toggle: Toggle): ToggleWithUsed => {
  let value = toggle();
  let used = !!site.storage.get(key);
  const novTog = (v?: boolean) => {
    if (defined(v)) {
      value = v;
      if (!used) {
        site.storage.set(key, '1');
        used = true;
      }
      toggle.effect(v);
    }
    return value;
  };
  novTog.toggle = () => novTog(!novTog());
  novTog.used = () => used;
  novTog.effect = toggle.effect;
  return novTog;
};
