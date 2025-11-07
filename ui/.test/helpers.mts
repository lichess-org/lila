import { test } from 'node:test';
import { existsSync } from 'node:fs';
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
const pkgRootCache = new Map<string, string | null>();

function findPkgRoot(parent: string): string | null {
  const start = dirname(fileURLToPath(parent));
  if (pkgRootCache.has(start)) return pkgRootCache.get(start)!;
  let dir = start;
  while (true) {
    if (existsSync(join(dir, 'package.json'))) {
      const href = pathToFileURL(dir + '/').href;
      pkgRootCache.set(dir, href);
      return href;
    }
    const parent = dirname(dir);
    if (parent === dir) {
      pkgRootCache.set(start, null);
      return null;
    }
    dir = parent;
  }
}

function hasExt(spec: string): boolean {
  return /\.[a-zA-Z0-9]+$/.test(spec);
}

function candidateUrls(baseHref: string, subpath: string): URL[] {
  // Map "@/foo/bar" -> "src/foo/bar[.ext]" within the package root
  const root = new URL(baseHref);
  const p = `src/${subpath.replace(/^\/+/, '')}`;
  const list: string[] = [];

  if (hasExt(p)) {
    list.push(p);
  } else {
    list.push(`${p}.ts`, `${p}.mts`);
  }
  return list.map(rel => new URL(rel, root));
}

export async function resolve(
  specifier: string,
  context: ResolveContext,
  next: NextResolver,
): Promise<ResolveResult> {
  if (specifier.startsWith('@/')) {
    const base = findPkgRoot(context.parentURL ?? import.meta.url);
    if (base) {
      const sub = specifier.slice(2);
      for (const url of candidateUrls(base, sub)) {
        if (existsSync(fileURLToPath(url))) {
          return { url: url.href, shortCircuit: true };
        }
      }
    }
  }

  return next(specifier, context);
}
