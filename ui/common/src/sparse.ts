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
