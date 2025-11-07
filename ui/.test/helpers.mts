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

type PkgInfo = { href: string; name: string } | false;
const pkgInfoCache = new Map<string, PkgInfo>();

function findPkg(parent: string): PkgInfo | false {
  const start = dirname(fileURLToPath(parent));
  if (pkgInfoCache.has(start)) return pkgInfoCache.get(start)!;

  let dir = start;
  while (true) {
    const pkgJsonFile = join(dir, 'package.json');
    if (existsSync(pkgJsonFile)) {
      const { name } = JSON.parse(readFileSync(pkgJsonFile, 'utf8')) as { name?: string };
      const info: PkgInfo = name ? { href: pathToFileURL(dir + '/').href, name } : false;
      pkgInfoCache.set(start, info);
      return info;
    }
    const up = dirname(dir);
    if (up === dir) {
      pkgInfoCache.set(start, false);
      return false;
    }
    dir = up;
  }
}

export async function resolve(
  specifier: string,
  context: ResolveContext,
  next: NextResolver,
): Promise<ResolveResult> {
  if (specifier.startsWith('@/')) {
    const base = context.parentURL ?? import.meta.url;
    const info = findPkg(base);
    if (info && info.name) {
      const sub = specifier.slice(2).replace(/^\/+/, '');
      // rewrite to a self-reference so tsx applies "exports" conditions
      const rewritten = `${info.name}/${sub}`;
      return next(rewritten, context);
    }
  }
  return next(specifier, context);
}
