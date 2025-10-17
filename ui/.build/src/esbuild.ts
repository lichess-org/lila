import es from 'esbuild';
import fs from 'node:fs';
import { join, basename } from 'node:path';
import { env, errorMark, warnMark, c } from './env.ts';
import { type Manifest, updateManifest } from './manifest.ts';
import { makeTask, stopTask } from './task.ts';
import { definedMap } from './algo.ts';

let esbuildCtx: es.BuildContext | undefined;

export async function esbuild(): Promise<any> {
  if (!env.begin('esbuild')) return;

  const options: es.BuildOptions = {
    bundle: true,
    metafile: true,
    treeShaking: true,
    splitting: true,
    format: 'esm',
    target: 'es2018',
    supported: { bigint: true },
    logLevel: 'silent',
    sourcemap: !env.prod,
    minify: env.prod,
    outdir: env.jsOutDir,
    entryNames: '[name].[hash]',
    chunkNames: 'lib.[hash]',
    plugins,
  };

  await fs.promises.mkdir(env.jsOutDir).catch(() => {});
  return Promise.all([
    inlineTask(),
    makeTask({
      key: 'bundle',
      ctx: 'esbuild',
      debounce: 300,
      noEnvStatus: true,
      globListOnly: true,
      includes: env.building.flatMap(pkg =>
        definedMap(
          pkg.bundle.map(bundle => bundle.module),
          path => ({ cwd: pkg.root, path }),
        ),
      ),
      execute: async entryPoints => {
        await esbuildCtx?.dispose();
        entryPoints.sort();
        esbuildCtx = await es.context({ ...options, entryPoints });
        if (env.watch) esbuildCtx.watch();
        else {
          await esbuildCtx.rebuild();
          await esbuildCtx.dispose();
        }
      },
    }),
  ]);
}

export async function stopEsbuild(): Promise<void> {
  stopTask(['bundle', 'inline']);
  await esbuildCtx?.dispose();
  esbuildCtx = undefined;
}

function inlineTask() {
  const js: Manifest = {};
  const inlineToModule: Record<string, string> = {};
  for (const [pkg, bundle] of env.tasks('bundle'))
    if (bundle.inline)
      inlineToModule[join(pkg.root, bundle.inline)] = bundle.module
        ? basename(bundle.module, '.ts')
        : basename(bundle.inline, '.inline.ts');
  return makeTask({
    key: 'inline',
    debounce: 300,
    includes: env.building.flatMap(pkg =>
      definedMap(
        pkg.bundle.map(b => b.inline),
        path => ({ cwd: pkg.root, path }),
      ),
    ),
    execute: (_, inlines) =>
      Promise.all(
        inlines.map(async inlineSrc => {
          const moduleName = inlineToModule[inlineSrc];
          try {
            const res = await es.transform(await fs.promises.readFile(inlineSrc), {
              minify: true,
              loader: 'ts',
              target: 'es2018',
            });
            esbuildLog(res.warnings);
            js[moduleName] ??= {};
            js[moduleName].inline = res.code;
          } catch (e) {
            if (e && typeof e === 'object' && 'errors' in e)
              esbuildLog((e as es.TransformFailure).errors, true);
            throw '';
          }
        }),
      ).then(() => updateManifest({ js })),
  });
}

function bundleManifest(meta: es.Metafile = { inputs: {}, outputs: {} }) {
  const js: Manifest = {};
  for (const [filename, info] of Object.entries(meta.outputs)) {
    const out = splitPath(filename);
    if (!out) continue;
    if (out.name === 'lib') {
      out.name = `lib.${out.hash}`;
      js[out.name] = {};
    } else js[out.name] = { hash: out.hash };
    const imports: string[] = [];
    for (const imp of info.imports) {
      if (imp.kind === 'import-statement') {
        const path = splitPath(imp.path);
        if (path) imports.push(`${path.name}.${path.hash}.js`);
      }
    }
    js[out.name].imports = imports;
  }
  updateManifest({ js });
}

function esbuildLog(msgs: es.Message[], error = false): void {
  for (const msg of msgs) {
    const file = msg.location?.file.replace(/^[./]*/, '') ?? '<unknown>';
    const line = msg.location?.line
      ? `:${msg.location.line}`
      : '' + (msg.location?.column ? `:${msg.location.column}` : '');
    const srcText = msg.location?.lineText;
    env.log(`${error ? errorMark : warnMark} - '${c.cyan(file + line)}' - ${msg.text}`, 'esbuild');
    if (srcText) env.log('  ' + c.magenta(srcText), 'esbuild');
  }
}

function splitPath(path: string) {
  const match = path.match(/\/public\/compiled\/(.*)\.([A-Z0-9]+)\.js$/);
  return match ? { name: match[1], hash: match[2] } : undefined;
}

// $trim and $html will only process characters between the first two backticks encountered
// so:
//   $html`     <div>    ${    x ?      `<- 2nd backtick   ${y}${z}` : ''    }     </div>`
//
// minifies (partially) to:
//   `<div> ${ x ? `<- 2nd backtick   ${y}${z}` : ''    }     </div>`
//
// nested template literals in interpolations are unchanged and still work, but they
// won't be minified.
//
// $trim condenses multiline strings formatted for source code readability into the user-friendly format
// by trimming each line and replacing single newlines

const plugins = [
  {
    name: 'condenseLiterals',
    setup(build: es.PluginBuild) {
      build.onLoad({ filter: /\.ts$/ }, async (args: es.OnLoadArgs) => ({
        loader: 'ts',
        contents: condenseLiterals(await fs.promises.readFile(args.path, 'utf8')),
      }));
    },
  },
  {
    name: 'onBundleDone',
    setup(build: es.PluginBuild) {
      build.onEnd(async (result: es.BuildResult) => {
        esbuildLog(result.errors, true);
        esbuildLog(result.warnings);
        env.begin('esbuild');
        env.done('esbuild', result.errors.length > 0 ? -3 : 0);
        if (result.errors.length === 0) bundleManifest(result.metafile!);
      });
    },
  },
];

// this may look ugly but it's 2-12x faster than regex scanning on V8, plus we can handle
// escaping correctly when a backtick is preceded by any odd number of backslashes
function condenseLiterals(text: string) {
  const backtick = '`'.charCodeAt(0);
  const nextLiteral = (from: number): [number, boolean] | undefined => {
    for (let i = text.indexOf('$', from); i >= 0; i = text.indexOf('$', i + 1)) {
      if (text.charCodeAt(i + 5) !== backtick) continue;
      if (text.startsWith('html', i + 1)) return [i + 6, true];
      if (text.startsWith('trim', i + 1)) return [i + 6, false];
    }
  };
  const condense = (str: string, isHtml: boolean) =>
    isHtml
      ? str.trim().replace(/\s+/g, ' ').replace(/>\s+</g, '><')
      : str
          .trim()
          .replace(/(?:\n[ \t]*){2,}/g, '\n\n')
          .replace(/\n[ \t]+/g, ' ');

  const out: string[] = [];

  let cursor = 0;
  for (let literal = nextLiteral(cursor); literal; literal = nextLiteral(++cursor)) {
    const [beginLiteral, isHtml] = literal;
    out.push(text.slice(cursor, beginLiteral - 6));

    for (
      cursor = text.indexOf('`', beginLiteral);
      cursor !== -1 && text[cursor - 1] === '\\';
      cursor = text.indexOf('`', cursor + 1)
    ) {
      let backslashes = 1;
      for (let i = cursor - 2; text[i] === '\\' && i >= beginLiteral; i--) backslashes++;
      if ((backslashes & 1) === 0) break;
    }
    if (cursor === -1) {
      cursor = beginLiteral - 1; // unterminated template literal, esbuild will report
      break;
    }
    out.push('`', condense(text.slice(beginLiteral, cursor), isHtml), '`');
  }
  if (cursor === 0) return text;
  out.push(text.slice(cursor));
  return out.join('');
}
