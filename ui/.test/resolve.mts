import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

type ResolveContext = { parentURL?: string; conditions: string[] };
type ResolveResult = { url: string; shortCircuit?: boolean; format?: string };
type NextResolver = (specifier: string, context: ResolveContext) => Promise<ResolveResult>;

const pkgNameCache = new Map<string, string | undefined>();

function findPkg(parent: string): string | undefined {
  const start = dirname(fileURLToPath(parent));
  if (pkgNameCache.has(start)) return pkgNameCache.get(start);

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
    const pkg = findPkg(context.parentURL ?? import.meta.url);
    if (pkg) return next(`${pkg}/${specifier.slice(2)}`, context);
  }

  const isFileUrl = specifier.startsWith('file:');
  if (!isFileUrl && !specifier.startsWith('.')) {
    return next(specifier, context);
  }

  const pathSpecifier = specifier.replace(/[?#].*$/, '');
  const suffix = specifier.slice(pathSpecifier.length);
  const base = context.parentURL ? dirname(fileURLToPath(context.parentURL)) : process.cwd();
  const file = isFileUrl ? fileURLToPath(pathSpecifier) : join(base, pathSpecifier);

  const candidates = pathSpecifier.endsWith('.js')
    ? [`${file.slice(0, -3)}.ts`]
    : /\.(?:[cm]?js|tsx?)$/.test(pathSpecifier)
      ? []
      : [`${file}.ts`, `${file}.js`];
  const match = candidates.find(existsSync);

  return next(match ? `${isFileUrl ? pathToFileURL(match).href : match}${suffix}` : specifier, context);
}
