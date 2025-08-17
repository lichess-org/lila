export const randomToken = (): string => {
  try {
    const data = globalThis.crypto.getRandomValues(new Uint8Array(9));
    return btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
  } catch (_) {
    return Math.random().toString(36).slice(2, 12);
  }
};

export function randomId(len = 8): string {
  const charSet32 = 'abcdefghkmnpqrstuvwxyz0123456789';
  const buffer = globalThis.crypto.getRandomValues(new Uint8Array(len));
  return Array.from(buffer, byte => charSet32[byte % 32]).join('');
}

export function clamp(value: number, bounds: { min?: number; max?: number }): number {
  return Math.max(bounds.min ?? -Infinity, Math.min(value, bounds.max ?? Infinity));
}

export function quantize(n: number | undefined, factor: number): number {
  return Math.round((n ?? 0) / factor) * factor;
}

export function shuffle<T>(arr: T[]): T[] {
  const shuffled = arr.slice();
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
}

export function deepFreeze<T>(obj: T): T {
  if (obj !== null && typeof obj === 'object')
    Object.values(obj)
      .filter(v => v !== null && typeof v === 'object')
      .forEach(o => deepFreeze(o));
  return Object.freeze(obj);
}

export function zip<T, U>(arr1: T[], arr2: U[]): [T, U][] {
  const length = Math.min(arr1.length, arr2.length);
  const result: [T, U][] = [];
  for (let i = 0; i < length; i++) {
    result.push([arr1[i], arr2[i]]);
  }
  return result;
}

export function definedMap<T, U>(arr: (T | undefined)[], fn: (v: T) => U | undefined): U[] {
  return arr.reduce<U[]>((acc, v) => {
    if (v === undefined) return acc;
    const result = fn(v);
    if (result !== undefined) acc.push(result);
    return acc;
  }, []);
}

export function definedUnique<T>(items: (T | undefined)[]): T[] {
  return [...new Set(items.filter((item): item is T => item !== undefined))];
}

/**
 * Comparison of enumerable primitives.
 * Complex properties get reference equality only.
 * If two vars have the same type and this type is in `excludedComparisonTypes`, then `true` is returned.
 */
export function isEquivalent(a: any, b: any, excludedComparisonTypes: string[] = []): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (excludedComparisonTypes.some(t => typeof a === t)) return true;
  if (Array.isArray(a))
    return (
      Array.isArray(b) &&
      a.length === b.length &&
      a.every((x, i) => isEquivalent(x, b[i], excludedComparisonTypes))
    );
  if (typeof a !== 'object') return false;
  const [aKeys, bKeys] = [Object.keys(a), Object.keys(b)];
  if (aKeys.length !== bKeys.length) return false;
  return aKeys.every(key => bKeys.includes(key) && isEquivalent(a[key], b[key], excludedComparisonTypes));
}

// true if a merge of sub into o would result in no change to o (structural containment)
export function isContained(o: any, sub: any): boolean {
  if (o === sub || sub === undefined) return true;
  if (typeof o !== typeof sub) return false;
  if (Array.isArray(o))
    return Array.isArray(sub) && o.length === sub.length && o.every((x, i) => isEquivalent(x, sub[i]));
  if (typeof o !== 'object') return false;
  const [aKeys, subKeys] = [Object.keys(o), Object.keys(sub)];
  if (aKeys.length < subKeys.length) return false;
  return subKeys.every(key => aKeys.includes(key) && isContained(o[key], sub[key]));
}

export function shallowSort(obj: { [key: string]: any }): { [key: string]: any } {
  const sorted: { [key: string]: any } = {};
  for (const key of Object.keys(obj).sort()) sorted[key] = obj[key];
  return sorted;
}
