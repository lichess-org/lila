import type { WatchEventType } from 'node:fs';
import fs from 'node:fs/promises';
import path from 'node:path';
import type { Context, PackageInfo, PackageName, PackagesWrap } from '@build/wrapper/types';
import { errorMsg } from '@build/wrapper/util';
import { type BuildContext, context } from 'esbuild';
import { isoldatedDtsPlugin } from 'esbuild-isolated-dts';
import fg from 'fast-glob';
import grfn from 'grfn';
import { graphWrap } from './graph.js';
import type { BuildConfig } from './types.js';

const target = 'es2017',
  srcDir = 'src',
  distDir = 'dist',
  bundleDir = 'build',
  publicDir = 'public/compiled';

export function esbuildContext(): Context {
  const ctxsMap = new Map<PackageName, BuildContext[]>();

  let bundleOutdir: string;
  let graph: (pkg?: PackageInfo) => Record<string, any>;

  let root: PackageInfo;
  let packages: PackageInfo[];
  let isProd: boolean;

  return {
    name: 'JS',

    async init(pWrap: PackagesWrap, flags: string[]): Promise<void> {
      root = pWrap.root;
      packages = pWrap.packages;
      isProd = flags.includes('--prod');

      bundleOutdir = `${root.path}/${publicDir}`;

      await fs.mkdir(bundleOutdir, { recursive: true });

      graph = graphWrap(packages, build);

      await Promise.all(
        packages.map(async pkg => {
          const ctxs = await initPackageContexts(pkg);
          ctxsMap.set(pkg.name, ctxs);
        }),
      );
    },

    packageWatch: [
      { path: srcDir, options: { recursive: true } },
      { path: 'package.json', options: {} },
      { path: 'tsconfig.json', options: {} },
    ],

    async run(pkg: PackageInfo, event: WatchEventType, filepath: string | null) {
      if (filepath?.endsWith('package.json') || event === 'rename') clearPkgContexts(pkg);
      await grfn(graph(pkg))();
    },

    async runAll() {
      await grfn(graph())();
    },

    async stop(): Promise<void> {
      const allCtxs = Array.from(ctxsMap.values()).flat();
      await Promise.all(allCtxs.map(ctx => ctx.cancel()));
      await Promise.all(allCtxs.map(ctx => ctx.dispose()));
      ctxsMap.clear();
    },
  };

  async function build(pkg: PackageInfo): Promise<void> {
    const ctxs = await getPkgContexts(pkg);
    await Promise.all(ctxs.map(ctx => ctx.cancel()));
    await Promise.all(ctxs.map(ctx => ctx.rebuild()));
  }

  async function clearPkgContexts(pkg: PackageInfo): Promise<void> {
    const ctxs = ctxsMap.get(pkg.name);
    if (ctxs) {
      await Promise.all(ctxs.map(ctx => ctx.cancel()));
      await Promise.all(ctxs.map(ctx => ctx.dispose()));
      ctxsMap.delete(pkg.name);
    }
  }

  async function getPkgContexts(pkg: PackageInfo): Promise<BuildContext[]> {
    const existing = ctxsMap.get(pkg.name);
    if (existing) return existing;

    const newCtxs = await initPackageContexts(pkg);
    ctxsMap.set(pkg.name, newCtxs);
    return newCtxs;
  }

  async function createCompileContext(pkg: PackageInfo): Promise<BuildContext> {
    const entryPoints = await fg(`${pkg.path}/${srcDir}/**/*`, {
      ignore: [`${pkg.path}/${srcDir}/${bundleDir}/**/*`],
    });
    return context({
      entryPoints: entryPoints,
      bundle: false,
      minify: false,
      sourcemap: 'linked',
      format: 'esm',
      charset: 'utf8',
      target,
      outdir: `${pkg.path}/${distDir}`,
      plugins: [isoldatedDtsPlugin()],
    });
  }

  function moduleName(input: string): string {
    return input
      .split(/[\.-]/)
      .map((part, index) => (index === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
      .join('');
  }

  function createBundleContext(pkg: PackageInfo, entryPoint: string): Promise<BuildContext> {
    const fileName = path.basename(entryPoint, '.ts'),
      pkgName = pkg.name.startsWith('@') ? pkg.name.split('/')[1] : pkg.name,
      name = fileName === 'main' ? pkgName : `${pkgName}.${fileName}`,
      lishogiName = `lishogi.${name}`,
      ext = isProd ? '.min.js' : '.js';

    return context({
      entryPoints: [entryPoint],
      bundle: true,
      target,
      format: 'iife',
      charset: 'utf8',
      sourcemap: !isProd ? undefined : undefined,
      minify: isProd,
      legalComments: 'none',
      define: { __bundlename__: `"${moduleName(name)}"` },
      outfile: `${bundleOutdir}/${lishogiName}${ext}`,
    });
  }

  async function initPackageContexts(
    pkg: PackageInfo & { lishogi?: BuildConfig },
  ): Promise<BuildContext[]> {
    const ctxs: BuildContext[] = [];

    try {
      if (pkg.lishogi?.compile) {
        const esmCtx = await createCompileContext(pkg);
        ctxs.push(esmCtx);
      }

      if (pkg.lishogi?.bundle) {
        const bundlePath = path.join(srcDir, bundleDir),
          allEntries: string[] = await fg(`${pkg.path}/${bundlePath}/*`);

        const bundleCtxs = await Promise.all(
          allEntries.map(entry => createBundleContext(pkg, entry)),
        );
        ctxs.push(...bundleCtxs);
      }

      return ctxs;
    } catch (error) {
      console.error(errorMsg(`Failed to create context for ${pkg.name}:`), error);
      return ctxs;
    }
  }
}
