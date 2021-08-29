import type { Square as Key } from 'chess.js';
import m from './mithrilFix';
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
  util.readKeys(blueprint.apples).forEach(function (key: Key) {
    items[key] = 'apple';
  });

  const get = function (key: Key) {
    return items[key];
  };

  const list = function () {
    return Object.values(items);
  };

  return {
    get: get,
    withItem: function <U>(key: Key, f: (item: 'apple') => U) {
      const item = items[key];
      if (item) return f(item);
      return;
    },
    remove: function (key: Key) {
      delete items[key];
    },
    hasItem: function (item: 'apple') {
      return list().includes(item);
    },
    appleKeys: function () {
      const keys: Key[] = [];
      for (const k in items) if (items[k as Key] === 'apple') keys.push(k as Key);
      return keys;
    },
  };
}

export function view(item: string) {
  return m('item.' + item);
}
