import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import { env, colors as c, errorMark, warnMark, lines } from './main.ts';
import { globArray } from './parse.ts';

let tscPs: cps.ChildProcessWithoutNullStreams | undefined;

export function stopTscExternal(): void {
  tscPs?.kill();
  tscPs = undefined;
}

export async function tscExternal(): Promise<void> {
  return new Promise((resolve, reject) =>
    (async () => {
      if (!env.tsc) return resolve();

      const cfg: any = { files: [] };
      const cfgPath = path.join(env.buildTempDir, 'build.tsconfig.json');

      cfg.references = (await globArray('*/tsconfig*.json', { cwd: env.uiDir }))
        .sort((a, b) => a.localeCompare(b))
        .filter(x => env.building.some(mod => x.startsWith(`${mod.root}/`)))
        .map(x => ({ path: x }));

      // verify that tsconfig references are correct
      for (const tsconfig of cfg.references) {
        if (!tsconfig?.path) continue;
        if (!fs.existsSync(tsconfig.path)) env.exit(`${errorMark} - Missing: '${c.cyan(tsconfig.path)}'`);
        const ref = JSON.parse(await fs.promises.readFile(tsconfig.path, 'utf8'));
        const module = path.basename(path.dirname(tsconfig.path));
        for (const dep of env.deps.get(module) ?? []) {
          if (!ref.references?.some((x: any) => x.path.endsWith(path.join(dep, 'tsconfig.json'))))
            env.warn(
              `${warnMark} - Module ${c.grey(module)} depends on ${c.grey(dep)} with no reference in '${c.cyan(
                module + '/tsconfig.json',
              )}'`,
              'tsc',
            );
        }
      }

      await fs.promises.writeFile(cfgPath, JSON.stringify(cfg));
      const thisPs = (tscPs = cps.spawn('.build/node_modules/.bin/tsc', [
        '-b',
        cfgPath,
        ...(env.watch ? ['-w', '--preserveWatchOutput'] : ['--incremental']),
      ]));

      env.log(`Compiling typescript`, { ctx: 'tsc' });

      thisPs.stdout?.on('data', (buf: Buffer) => {
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
      thisPs.stderr?.on('data', txt => env.log(txt, { ctx: 'tsc', error: true }));
      thisPs.addListener('close', code => {
        thisPs.removeAllListeners();
        if (code !== null) env.done(code, 'tsc');
        if (code === 0) resolve();
        else reject();
      });
    })(),
  );
}

function tscLog(text: string): void {
  if (text.includes('File change detected') || text.includes('Starting compilation')) return; // redundant
  text = text.replace(/\d?\d:\d\d:\d\d (PM|AM) - /, '');
  if (text.match(/: error TS\d\d\d\d/)) text = fixTscError(text);
  if (text.match(/Found (\d+) errors?/)) {
    if (env.watch) env.done(1, 'tsc');
  } else env.log(text, { ctx: 'tsc' });
}

const fixTscError = (text: string) =>
  // format location for vscode embedded terminal ctrl click
  text
    .replace(/^[./]*/, `${errorMark} - '\x1b[36m`)
    .replace(/\.ts\((\d+),(\d+)\):/, ".ts:$1:$2\x1b[0m' -")
    .replace(/error (TS\d{4})/, '$1');
