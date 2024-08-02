import type { SquareName as Key } from 'chessops';
import * as util from './util';

export interface Items {
  doIfKeyExists<U>(key: Key, f: () => U): U | undefined;
  remove(key: Key): void;
  isEmpty(): boolean;
  appleKeys(): Key[];
}

export function ctrl(blueprint: { apples: string | Key[] }): Items {
  const items: Set<Key> = new Set(util.readKeys(blueprint.apples));

  return {
    doIfKeyExists: <U>(key: Key, f: () => U) => (items.has(key) ? f() : undefined),
    remove: (key: Key) => items.delete(key),
    isEmpty: () => items.size == 0,
    appleKeys: () => Array.from(items),
  };
}
