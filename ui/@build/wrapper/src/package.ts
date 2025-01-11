import { execSync } from 'node:child_process';
import { promises as fs } from 'node:fs';
import path from 'node:path';
import type { PackageInfo, PackagesWrap } from './types.js';

interface PackageJson {
  name: string;
  dependencies?: Record<string, string>;
  lishogi: Record<string, any>;
}

function getAllPackagesPath(): string[] {
  const output = execSync('pnpm m ls --depth -1 --parseable').toString();
  return output.split('\n').filter(o => !!o);
}

function getFiltered(args: string[]): string[] {
  const filterIndex = args.findIndex(arg => arg.startsWith('--filter'));
  if (filterIndex === -1) return [];

  const result: string[] = [];
  const filterArg = args[filterIndex];

  if (filterArg.includes('=')) {
    result.push(...filterArg.split('=')[1].split(','));
  } else {
    for (let i = filterIndex + 1; i < args.length; i++) {
      const value = args[i];
      if (value.startsWith('--')) break;
      result.push(...value.split(','));
    }
  }

  return result;
}

function isRootPkg(pkg: PackageInfo): boolean {
  return pkg.name === 'lishogi';
}

export async function getAllPackages(flags: string[]): Promise<PackagesWrap> {
  const paths = getAllPackagesPath();
  const filtered = getFiltered(flags);
  const packages: PackageInfo[] = (
    await Promise.all(
      paths.map(async (pkgPath): Promise<PackageInfo | null> => {
        try {
          const rawData = await fs.readFile(path.join(pkgPath, 'package.json'), 'utf8');
          const parsed = JSON.parse(rawData) as PackageJson;

          return {
            name: parsed.name,
            deps: Object.keys(parsed.dependencies || {}),
            revDeps: [],
            path: pkgPath,
            lishogi: parsed.lishogi,
          };
        } catch (error) {
          console.error(`Error reading package.json from ${pkgPath}:`, error);
          return null;
        }
      }),
    )
  )
    .filter(pkg => !!pkg)
    .filter(pkg => !!pkg.lishogi || isRootPkg(pkg));

  packages.forEach(pkg => {
    pkg.deps = pkg.deps.filter(d => packages.some(pkg2 => pkg2.name === d));
    pkg.deps.forEach(depName => {
      const depPackage = packages.find(p => p.name === depName);
      if (depPackage) {
        depPackage.revDeps.push(pkg.name);
      }
    });
  });

  const root = packages.find(pkg => isRootPkg(pkg))!;

  return {
    root,
    packages: packages.filter(
      pkg =>
        pkg.name !== root.name &&
        !root.deps.includes(pkg.name) &&
        (!filtered.length || filtered.includes(pkg.name)),
    ),
  };
}
