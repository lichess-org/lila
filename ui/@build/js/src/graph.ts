import type { PackageInfo, PackageName } from '@build/wrapper/types';
import { withColor } from '@build/wrapper/util';

export const graphWrap =
  (pkgs: PackageInfo[], fn: (pkg: PackageInfo) => Promise<void>) =>
  (changedPkg?: PackageInfo): Record<string, any> => {
    const collectAffected = (
      name: PackageName,
      visited = new Set<PackageName>(),
    ): Set<PackageName> => {
      const pkg = pkgs.find(p => p.name === name);
      if (pkg && !visited.has(name)) {
        visited.add(name);
        pkg.revDeps.forEach(dep => collectAffected(dep, visited));
      }
      return visited;
    };
    const affected = changedPkg ? collectAffected(changedPkg.name) : new Set(pkgs.map(p => p.name));

    console.log(
      `Building ${affected.size}/${pkgs.length} packages:`,
      Array.from(affected)
        .map(a => `${withColor(a)}`)
        .join(', '),
    );
    const g: Record<string, any> = {};
    for (const pkg of pkgs.filter(p => affected.has(p.name))) {
      const affectedDeps = pkg.deps.filter(d => affected.has(d));
      if (affectedDeps.length) {
        g[pkg.name] = [
          async () => {
            await fn(pkg);
          },
          affectedDeps,
        ];
      } else {
        g[pkg.name] = async () => {
          await fn(pkg);
        };
      }
    }
    // Final node with dependency on everything, otherwise we return too soon
    g._all_ = [() => {}, affected];

    return g;
  };
