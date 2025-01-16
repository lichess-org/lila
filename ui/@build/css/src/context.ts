import fs from 'node:fs';
import path from 'node:path';
import type { Context, PackageInfo, PackagesWrap } from '@build/wrapper/types';
import autoprefixer from 'autoprefixer';
import cssnano from 'cssnano';
import fg from 'fast-glob';
import postcss from 'postcss';
import * as sass from 'sass-embedded';
import { SassGraph } from 'sass-graph-ts';
import { build } from './css-variables/builder.js';
import { extractVariables } from './css-variables/extractor.js';

const sassOptions: sass.Options<'async'> = {
  quietDeps: false,
  silenceDeprecations: [],
  style: 'expanded',
  verbose: true,
};

const loadPaths = ['ui/@build/css/node_modules', ''];

export function sassContext(): Context {
  let outdir: string;

  const allFiles = new Map<string, string[]>();
  const buildFiles = new Map<string, string[]>();

  let themeDir: string | undefined;
  let graph: SassGraph;
  let extracted: Set<string>;

  let compiler: sass.AsyncCompiler;
  let processor: postcss.Processor;

  let rootPath: string;
  let packages: PackageInfo[];
  let isProd: boolean;
  let isFiltered: boolean;
  let onlyVars: boolean;

  return {
    name: 'CSS',

    async init(pWrap: PackagesWrap, flags: string[]): Promise<void> {
      rootPath = pWrap.root.path;
      packages = pWrap.packages;
      isProd = flags.includes('--prod');
      isFiltered = flags.includes('--filter');
      onlyVars = flags.includes('--vars');

      outdir = `${rootPath}/public/css/`;
      await fs.promises.mkdir(outdir, { recursive: true });

      await innerInit();

      const unusedFiles = findUnusedFiles();
      if (unusedFiles.length) console.warn(`Unused files:\n'${unusedFiles.join('\n')}\n`);

      themeDir = packages.find(p => p.name === 'common')?.path;
      await buildVars();

      compiler = await sass.initAsyncCompiler();

      const postCssPlugins: postcss.AcceptedPlugin[] = flags.includes('--no-plugins')
        ? []
        : [
            autoprefixer({ remove: false }),
            cssnano({
              preset: [
                'default',
                {
                  normalizeWhitespace: isProd,
                  colormin: { precision: 3 },
                  discardComments: { removeAll: isProd },
                },
              ],
            }),
          ];
      processor = postcss(postCssPlugins);

      sassOptions.loadPaths = loadPaths.map(p => path.join(rootPath, p));
      sassOptions.importers = [new sass.NodePackageImporter()];

      if (!isProd) sassOptions.sourceMap = true;
    },

    packageWatch: [{ path: 'css', options: { recursive: true } }],

    async run(pkg: PackageInfo, event: fs.WatchEventType, filepath: string) {
      if ((pkg.name === 'common' && filepath.includes('theme/gen')) || onlyVars) return;
      if (event === 'rename') {
        console.log('Reinitiating build files and graph dependency');
        await innerInit();
      } else updateGraph(filepath);
      const newExtracted = await extractVariables([filepath]);
      const allAffected = recImports(filepath);
      const rebuildFiles = filterBuildFiles(allAffected);
      if (Array.from(newExtracted).some(e => !extracted.has(e))) {
        console.log('Rebuilding variable files...');
        await buildVars();
      }
      console.log(`Building ${rebuildFiles.length} css files...`);
      await Promise.all(rebuildFiles.map(file => processFile(packageNameOfFile(file), file)));
    },

    async runAll() {
      if (onlyVars) return;
      console.log('Building all css files...');

      await Promise.all(
        Array.from(buildFiles).flatMap(([name, files]) =>
          files.map(file => processFile(name, file)),
        ),
      );
    },

    async stop(): Promise<void> {
      await compiler.dispose();
    },
  };

  async function innerInit() {
    allFiles.clear();
    buildFiles.clear();

    await Promise.all(
      packages.map(async pkg => {
        const name = pkg.name.startsWith('@') ? pkg.name.split('/')[1] : pkg.name;
        const files = await fg(`${pkg.path}/css/**/*.scss`);
        allFiles.set(name, files);
        buildFiles.set(name, filterBuildFiles(files));
      }),
    );

    initGraph();
  }

  async function buildVars(): Promise<void> {
    if (themeDir && !isFiltered) {
      extracted = await extractVariables(Array.from(allFiles.values()).flat());
      await build(extracted, path.join(themeDir, 'css/theme'));
    } else
      console.log(
        'Skipping css variable build (--filter flag was provided or theme dir was not found)',
      );
  }

  async function processFile(packageName: string, file: string): Promise<void> {
    const sassResult = await compiler.compileAsync(file, sassOptions);
    const postCssResult = await processor.process(sassResult.css, { from: file });

    for (const warning of postCssResult.warnings()) {
      console.warn(warning.toString());
    }

    const basename = path.basename(file, '.scss');
    const name = basename === 'main' ? packageName : `${packageName}.${basename}`;
    const outputPath = path.join(outdir, `${name}.${isProd ? 'min' : 'dev'}.css`);

    let res = postCssResult.css;
    if (!isProd && sassResult.sourceMap)
      res = res + '\n'.repeat(2) + createSourceMap(sassResult.sourceMap);

    await fs.promises.writeFile(outputPath, res);
  }

  function createSourceMap(sourceMap: Exclude<sass.CompileResult['sourceMap'], undefined>): string {
    const sm = JSON.stringify(sourceMap);
    const smBase64 = (Buffer.from(sm, 'utf8') || '').toString('base64');
    return `/*# sourceMappingURL=data:application/json;charset=utf-8;base64,${smBase64} */`;
  }

  function initGraph(): void {
    graph = SassGraph.parseDir(`${rootPath}/ui/`, {
      extensions: ['scss'],
      resolver: importPath => {
        const match = importPath.match(/^pkg:([^/]+)\/(.+)$/);
        if (match) {
          const packageName = match[1];
          let restOfPath = match[2];
          const basePath = path.join(rootPath, 'ui', packageName, 'css');

          if (restOfPath === 'theme') restOfPath = 'theme/gen/theme';

          const firstPath = path.join(
            basePath,
            path.dirname(restOfPath),
            `_${path.basename(restOfPath)}.scss`,
          );
          if (fs.existsSync(firstPath)) return firstPath;

          const fallbackPath = path.join(basePath, `${restOfPath}.scss`);
          if (fs.existsSync(fallbackPath)) return fallbackPath;
        }

        return false;
      },
    })!;
  }

  function updateGraph(path: string): void {
    if (!graph.index[path]) initGraph();
    else {
      const fileImports = SassGraph.parseFile(path, { extensions: ['scss'] }).index[path].imports;

      for (const dep of graph.index[path].imports) {
        if (!fileImports.includes(dep)) {
          graph.index[dep].importedBy = graph.index[dep].importedBy.filter(d => d !== path);
        }
      }
      for (const dep of fileImports) {
        const depImportedBy = graph.index[dep].importedBy;
        if (!depImportedBy.includes(path)) {
          graph.index[dep].importedBy.push(path);
        }
      }

      graph.index[path].imports = fileImports;
    }
  }

  function recImports(path: string): string[] {
    const paths = graph.index[path].importedBy;
    const arr = [path];
    paths.forEach(p => {
      arr.push(...recImports(p));
    });
    return arr;
  }

  function findUnusedFiles(): string[] {
    const usedFiles = new Set<string>();

    function traverse(file: string) {
      if (usedFiles.has(file)) return;
      usedFiles.add(file);

      const dependencies = graph.index[file]?.imports || [];
      for (const dep of dependencies) {
        traverse(dep);
      }
    }

    for (const buildFile of Array.from(buildFiles.values()).flat()) {
      traverse(buildFile);
    }
    return Array.from(allFiles.values())
      .flat()
      .filter(file => !usedFiles.has(file) && !file.includes('css/theme'));
  }

  function filterBuildFiles(files: string[]): string[] {
    return files.filter(
      file => path.basename(path.dirname(file)) === 'build' && !path.basename(file).startsWith('_'),
    );
  }

  function packageNameOfFile(file: string): string {
    return Array.from(allFiles).find(([_, files]) => files.includes(file))![0];
  }
}
