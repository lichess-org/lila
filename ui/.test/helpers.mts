import { test } from 'node:test';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

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

// this next bit defines a loader to map "@/..." to "lib/src/..." when importing using @ alias

type ResolveContext = { parentURL?: string; conditions: string[] };
type ResolveResult = { url: string; shortCircuit?: boolean; format?: string };
type NextResolver = (specifier: string, context: ResolveContext) => Promise<ResolveResult>;

const pkgNameCache = new Map<string, string | undefined>();

function findPkg(parent: string): string | undefined {
  const start = dirname(fileURLToPath(parent));
  if (pkgNameCache.has(start)) return pkgNameCache.get(start)!;

  let dir = start;
  while (true) {
    const pkgJsonFile = join(dir, 'package.json');
    if (existsSync(pkgJsonFile)) {
      const { name } = JSON.parse(readFileSync(pkgJsonFile, 'utf8')) as { name?: string };
      pkgNameCache.set(start, name);
      return name;
    }
    const parent = dirname(dir);
    if (parent === dir) {
      pkgNameCache.set(start, undefined);
      return undefined;
    }
    dir = parent;
  }
}

export async function resolve(
  specifier: string,
  context: ResolveContext,
  next: NextResolver,
): Promise<ResolveResult> {
  if (specifier.startsWith('@/')) {
    const name = findPkg(context.parentURL ?? import.meta.url);
    if (name) {
      // rewrite to a self-reference so tsx applies "exports" conditions
      const rewritten = `${name}/${specifier.slice(2).replace(/^\/+/, '')}`;
      return next(rewritten, context);
    }
  }
  return next(specifier, context);
}
