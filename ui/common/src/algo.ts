export const randomToken = (): string => {
  try {
    const data = window.crypto.getRandomValues(new Uint8Array(9));
    return btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
  } catch (_) {
    return Math.random().toString(36).slice(2, 12);
  }
};

export function clamp(value: number, bounds: { min?: number; max?: number }): number {
  return Math.max(bounds.min ?? -Infinity, Math.min(value, bounds.max ?? Infinity));
}

export function shuffle<T>(array: T[]): T[] {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

export function deepFreeze<T>(obj: T): T {
  if (obj !== null && typeof obj === 'object')
    Object.values(obj)
      .filter(v => v !== null && typeof v === 'object')
      .forEach(o => deepFreeze(o));
  return Object.freeze(obj);
}

// recursive comparison of enumerable primitives. complex properties get reference equality only
export function isEquivalent(a: any, b: any): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) {
    return Array.isArray(b) && a.length === b.length && a.every((x, i) => isEquivalent(x, b[i]));
  }
  if (typeof a !== 'object') return false;
  const [aKeys, bKeys] = [Object.keys(a), Object.keys(b)];
  if (aKeys.length !== bKeys.length) return false;
  for (const key of aKeys) {
    if (!bKeys.includes(key) || !isEquivalent(a[key], b[key])) return false;
  }
  return true;
}

export function zip<T, U>(arr1: T[], arr2: U[]): [T, U][] {
  const length = Math.min(arr1.length, arr2.length);
  const result: [T, U][] = [];
  for (let i = 0; i < length; i++) {
    result.push([arr1[i], arr2[i]]);
  }
  return result;
}

export function findMapped<T, U>(arr: T[], callback: (el: T) => U | undefined): U | undefined {
  for (const el of arr) {
    const result = callback(el);
    if (result) return result;
  }
  return undefined;
}

export type SparseSet<T> = Set<T> | T;
export type SparseMap<V> = Map<string, SparseSet<V>>;

export function spread<T>(v: undefined | SparseSet<T>): T[] {
  return v === undefined ? [] : v instanceof Set ? [...v] : [v];
}

export function spreadMap<T>(m: SparseMap<T>): [string, T[]][] {
  return [...m].map(([k, v]) => [k, spread(v)]);
}

export function getSpread<T>(m: SparseMap<T>, key: string): T[] {
  return spread(m.get(key));
}

export function remove<T>(m: SparseMap<T>, key: string, val: T): void {
  const v = m.get(key);
  if (v === val) m.delete(key);
  else if (v instanceof Set) v.delete(val);
}

export function pushMap<T>(m: SparseMap<T>, key: string, val: T): void {
  const v = m.get(key);
  if (!v) m.set(key, val);
  else {
    if (v instanceof Set) v.add(val);
    else if (v !== val) m.set(key, new Set([v as T, val]));
  }
}
