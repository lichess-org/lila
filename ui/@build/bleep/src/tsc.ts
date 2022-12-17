import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { buildModules } from './build';
import { env, colors as c, errorMark, lines } from './main';

export async function makeBleepConfig(): Promise<void> {
  await fs.promises.rm(env.tsconfigDir, { recursive: true, force: true });
  await fs.promises.mkdir(env.tsconfigDir);

  const cfg: any = { references: [] };

  for (const mod of buildModules) {
    if (env.tsc && (mod.tscOptions || !mod.bundle)) cfg.references.push({ path: path.join(mod.root, 'tsconfig.json') });
  }
  await fs.promises.writeFile(path.join(env.tsconfigDir, 'bleep.tsconfig.json'), JSON.stringify(cfg));
}

export function tsc(onSuccess: () => void) {
  const tsc = cps.spawn(
    'tsc',
    ['-b', path.join(env.tsconfigDir, 'bleep.tsconfig.json')].concat(
      env.watch ? ['-w', '--preserveWatchOutput'] : ['--force']
    ),
    { cwd: env.tsconfigDir }
  );
  env.log(`Checking typescript`, { ctx: 'tsc' });

  tsc.stdout?.on('data', (buf: Buffer) => {
    // no way to magically get build events...
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) {
      if (txt.includes('Found 0 errors')) {
        if (!env.exitCode.has('tsc')) onSuccess();
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
