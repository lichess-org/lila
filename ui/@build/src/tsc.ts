import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { buildModules } from './build';
import { env, colors as c, errorMark, lines } from './main';

export async function tsc(onSuccess: () => void) {
  if (!env.tsc) return onSuccess();
  let successCallbackTriggered = false;

  const cfgPath = path.join(env.buildDir, 'dist', 'build.tsconfig.json');
  const cfg: any = { files: [] };
  cfg.references = buildModules
    .filter(x => x.hasTsconfig)
    .map(x => ({ path: path.join(x.root, 'tsconfig.json') }));
  await fs.promises.writeFile(cfgPath, JSON.stringify(cfg));

  const tsc = cps.spawn(
    'tsc',
    ['-b', cfgPath].concat(env.watch ? ['-w', '--preserveWatchOutput'] : ['--force'])
  );

  env.log(`Checking typescript`, { ctx: 'tsc' });

  tsc.stdout?.on('data', (buf: Buffer) => {
    // no way to magically get build events...
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) {
      if (txt.includes('Found 0 errors')) {
        if (!successCallbackTriggered) {
          onSuccess();
          successCallbackTriggered = true;
        }
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
    } else onSuccess();
  });
}

function tscLog(text: string): void {
  if (text.includes('File change detected') || text.includes('Starting compilation')) return; // redundant
  text = text.replace(/\d?\d:\d\d:\d\d (PM|AM) - /, '');
  if (text.match(/: error TS\d\d\d\d/)) {
    // strip the ../../../../.. junk, highlight error
    text = `${errorMark} - ${text.replace(/^[./]*/, '')}`;
  }
  env.log(text.replace('. Watching for file changes.', ` - ${c.grey('Watching')}...`), { ctx: 'tsc' });
}
