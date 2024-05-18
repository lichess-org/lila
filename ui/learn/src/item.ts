import type { Square as Key } from 'chess.js';
import * as util from './util';

export interface Items<T> {
  get(key: Key): T | undefined;
  withItem<U>(key: Key, f: (item: T) => U): U | undefined;
  remove(key: Key): void;
  hasItem(item: T): boolean;
  appleKeys(): Key[];
}

export function ctrl(blueprint: { apples: string | Key[] }): Items<'apple'> {
  const items: Partial<Record<Key, 'apple'>> = {};
  util.readKeys(blueprint.apples).forEach((key: Key) => {
    items[key] = 'apple';
  });

  return {
    get: (key: Key) => items[key],
    withItem: <U>(key: Key, f: (item: 'apple') => U) => {
      const item = items[key];
      if (item) return f(item);
      return;
    },
    remove: (key: Key) => {
      delete items[key];
    },
    hasItem: (item: 'apple') => Object.values(items).includes(item),
    appleKeys: () => {
      const keys: Key[] = [];
      for (const k in items) if (items[k as Key] === 'apple') keys.push(k as Key);
      return keys;
    },
  };
}
