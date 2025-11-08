import { test } from 'node:test';

export function each<T extends any[]>(cases: readonly T[]) {
  return (name: string, fn: (...args: T) => void) => {
    for (const args of cases) {
      const label = `${name}: ${args.map(a => String(a)).join(' | ')}`;
      test(label, () => fn(...args));
    }
  };
}
export async function freshImport<T = any>(specifier: string): Promise<T> {
  const u = new URL(`../${specifier}`, import.meta.url);
  u.searchParams.set('t', String(Date.now()));
  return (await import(u.href)) as T;
}
