import { defined } from './common';

export interface StoredProp<T> {
  (): string;
  (v: T): void;
}

export interface StoredBooleanProp {
  (): boolean;
  (v: boolean): void;
}

const storage = window.lishogi.storage;

export function storedProp(k: string, defaultValue: boolean): StoredBooleanProp;
export function storedProp<T>(k: string, defaultValue: T): StoredProp<T>;
export function storedProp(k: string, defaultValue: any) {
  const sk = 'analyse.' + k,
    isBoolean = defaultValue === true || defaultValue === false;
  let value: any;
  return function (v: any) {
    if (defined(v) && v != value) {
      value = v + '';
      storage.set(sk, v);
    } else if (!defined(value)) {
      value = storage.get(sk);
      if (value === null) value = defaultValue + '';
    }
    return isBoolean ? value === 'true' : value;
  };
}

export interface StoredJsonProp<T> {
  (): T;
  (v: T): T;
}

export const storedJsonProp =
  <T>(key: string, defaultValue: () => T): StoredJsonProp<T> =>
  (v?: T) => {
    if (defined(v)) {
      storage.set(key, JSON.stringify(v));
      return v;
    }
    const ret = JSON.parse(storage.get(key)!);
    return ret !== null ? ret : defaultValue();
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
