import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { buildModules } from './build';
import { env, colors as c, errorMark, lines } from './main';

export async function tsc(): Promise<void> {
  return new Promise(resolve => {
    if (!env.tsc) return resolve();
    const cfgPath = path.join(env.buildDir, 'dist', 'build.tsconfig.json');
    const cfg: any = { files: [] };
    cfg.references = buildModules
      .filter(x => x.hasTsconfig)
      .map(x => ({ path: path.join(x.root, 'tsconfig.json') }));
    fs.writeFileSync(cfgPath, JSON.stringify(cfg));

    const tsc = cps.spawn(
      'tsc',
      ['-b', cfgPath].concat(env.watch ? ['-w', '--preserveWatchOutput'] : ['--force']),
    );

    env.log(`Checking typescript`, { ctx: 'tsc' });

    tsc.stdout?.on('data', (buf: Buffer) => {
      // no way to magically get build events...
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
    tsc.stderr?.on('data', txt => env.log(txt, { ctx: 'tsc', error: true }));
    tsc.addListener('close', code => {
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
  text
    .replace(/^[./]*/, `${errorMark} - '\x1b[36m`)
    .replace(/\.ts\((\d+),(\d+)\):/, ".ts:$1:$2\x1b[0m' -")
    .replace(/error (TS\d{4})/, '$1');
// format location for vscode embedded terminal ctrl click
