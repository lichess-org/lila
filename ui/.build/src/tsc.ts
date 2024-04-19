import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { env, colors as c, errorMark, warnMark, lines } from './main';

let tscPs: cps.ChildProcessWithoutNullStreams | undefined;

export function stopTsc() {
  tscPs?.removeAllListeners();
  tscPs?.kill();
  tscPs = undefined;
}

export async function tsc(): Promise<void> {
  return new Promise(async resolve => {
    if (!env.tsc) return resolve();

    const cfgPath = path.join(env.buildDir, 'dist', 'build.tsconfig.json');
    const cfg: any = { files: [] };
    cfg.references = env.building
      .filter(x => x.hasTsconfig)
      .sort((a, b) => a.name.localeCompare(b.name))
      .map(x => ({ path: path.join(x.root, 'tsconfig.json') }));

    // verify that tsconfig references are correct
    for (const tsconfig of cfg.references) {
      if (!tsconfig?.path) continue;
      if (!fs.existsSync(tsconfig.path)) env.exit(`${errorMark} - Missing: '${c.cyan(tsconfig.path)}'`);
      const ref = JSON.parse(await fs.promises.readFile(tsconfig.path, 'utf8'));
      const module = path.basename(path.dirname(tsconfig.path));
      for (const dep of env.deps.get(module) ?? []) {
        if (!ref.references?.some((x: any) => x.path.endsWith(path.join(dep, 'tsconfig.json'))))
          env.warn(
            `${warnMark} - Module '${c.grey(module)}' depends on '${c.grey(
              dep,
            )}' but no reference in '${c.cyan(tsconfig.path.slice(env.uiDir.length + 1))}'`,
          );
      }
    }

    await fs.promises.writeFile(cfgPath, JSON.stringify(cfg));
    tscPs = cps.spawn('.build/node_modules/.bin/tsc', [
      '-b',
      cfgPath,
      ...(env.watch ? ['-w', '--preserveWatchOutput'] : []),
    ]);

    env.log(`Compiling typescript`, { ctx: 'tsc' });

    tscPs.stdout?.on('data', (buf: Buffer) => {
      const txts = lines(buf.toString('utf8'));
      for (const txt of txts) {
        if (txt.includes('Found 0 errors')) {
          resolve();
          env.done(0, 'tsc');
        } else {
          tscLog(txt);
        }
      }
    });
    tscPs.stderr?.on('data', txt => env.log(txt, { ctx: 'tsc', error: true }));
    tscPs.addListener('close', code => {
      env.done(code || 0, 'tsc');
      if (code) {
        env.done(code, 'esbuild'); // fail both
      } else resolve();
    });
  });
}

function tscLog(text: string): void {
  if (text.includes('File change detected') || text.includes('Starting compilation')) return; // redundant
  text = text.replace(/\d?\d:\d\d:\d\d (PM|AM) - /, '');
  if (text.match(/: error TS\d\d\d\d/)) text = fixTscError(text);
  env.log(text.replace('. Watching for file changes.', ` - ${c.grey('Watching')}...`), { ctx: 'tsc' });
}

const fixTscError = (text: string) =>
  // format location for vscode embedded terminal ctrl click
  text
    .replace(/^[./]*/, `${errorMark} - '\x1b[36m`)
    .replace(/\.ts\((\d+),(\d+)\):/, ".ts:$1:$2\x1b[0m' -")
    .replace(/error (TS\d{4})/, '$1');
