import type { SquareName } from 'chessops';
import { readKeys } from './util';

export interface Items {
  doIfKeyExists<U>(key: SquareName, f: () => U): U | undefined;
  remove(key: SquareName): void;
  isEmpty(): boolean;
  appleKeys(): SquareName[];
}

export function ctrl(blueprint: { apples: string | SquareName[] }): Items {
  const items: Set<SquareName> = new Set(readKeys(blueprint.apples));

  return {
    doIfKeyExists: <U>(key: SquareName, f: () => U) => (items.has(key) ? f() : undefined),
    remove: (key: SquareName) => items.delete(key),
    isEmpty: () => items.size === 0,
    appleKeys: () => Array.from(items),
  };
}
