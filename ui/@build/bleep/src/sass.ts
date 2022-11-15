import * as cps from 'node:child_process';
import * as fs from 'node:fs';
import * as ps from 'node:process';
import * as path from 'node:path';
import { env, colors as c, lines, errorMark } from './main';
import { globArray } from './parse';

const importMap = new Map<string, Set<string>>(); // (cssFile, sourcesThatImportIt)
const debounceMap = new Map<string, NodeJS.Timeout>(); // fs.watch platform idiosyncracies

export async function sassWatch(): Promise<void> {
  debounceMap.forEach((timeout: NodeJS.Timeout) => clearTimeout(timeout));
  debounceMap.clear();
  importMap.clear();

  await buildThemedScss();

  const themedSources = new Set<string>(await globArray('./*/css/**/[^_]*.scss', { abs: false }));

  for (const dir of [...importMap.keys()].map(d => path.dirname(d))) {
    const watcher = fs.watch(dir);
    watcher.on('change', onChanges.bind(null, themedSources, dir));
    watcher.on('error', (err: Error) => env.error(err, 'sass'));
    watcher.on('close', () => {
      env.error('Watch done. Exiting', 'sass');
      ps.exit(-1);
    });
  }
  env.log('Building css', { ctx: 'sass' });
  sassBuild([...themedSources], false);
  return;
}

const partialRe = /(.*)\/_(.*)\.scss$/;

async function buildThemedScss() {
  env.log('Building themed scss', { ctx: 'sass' });

  const partials: string[] = (await globArray('./*/css/build/_*.scss', { abs: false })).filter(
    (f: string) => !f.endsWith('.abstract.scss')
  );
  for (const file of partials) {
    const match = partialRe.exec(file);
    if (!match || match.length < 3) continue;
    const path = match[1];
    const partial = match[2];
    for (const direction of ['ltr', 'rtl']) {
      for (const theme of ['light', 'dark', 'transp']) {
        const themed = `${path}/${partial}.${direction}.${theme}.scss`;
        if (fs.existsSync(themed)) {
          await parseImports(themed);
          continue;
        }
        const code =
          `@import '../../../common/css/dir/${direction}';\n` +
          `@import '../../../common/css/theme/${theme}';\n` +
          `@import '${partial}';\n`;
        try {
          await fs.promises.writeFile(themed, code);
          await parseImports(themed);
        } catch (e) {
          env.log(e, { error: true });
        }
      }
    }
  }
}

const params = ['--no-error-css', '--stop-on-error', '--no-color', '--quiet', '--quiet-deps'];

function sassBuild(sources: string[], tellTheWorld = true) {
  if (tellTheWorld) {
    for (const srcFile of sources) {
      env.log(`Building '${c.cyan(srcFile)}'`, { ctx: 'sass' });
    }
  }
  const sassExec = path.join(env.bleepDir, 'dart-sass', `${ps.platform}-${ps.arch}`, 'sass');

  const proc = cps.spawn(sassExec, params.concat(sources.map(sassArgument)));

  proc.stdout?.on('data', (buf: Buffer) => {
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) env.log(c.red(txt), { ctx: 'sass' });
  });
  proc.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
  proc.on('close', (code: number) => {
    if (code === 0) env.good('sass');
    else env.log(`${c.red('Errors found')} - ${c.grey('Watching')}...`, { ctx: 'sass' });
  });
}

const sassArgument = (src: string) =>
  `${src}:${path.join(env.cssDir, src.slice(src.lastIndexOf(path.sep) + 1).replace(/(.*)scss$/, '$1dev.css'))}`;

const hrule = '---------------------------------------------------------------------------';

function sassError(error: string) {
  for (const err of lines(error)) {
    if (err.startsWith('Error:')) {
      env.log(c.grey(hrule), { ctx: 'sass' });
      env.log(`${errorMark} -${err.slice(7)}`, { ctx: 'sass' });
    } else env.log(err, { ctx: 'sass' });
  }
}

function imports(srcFile: string, bset = new Set<string>()): Set<string> {
  if (bset.has(srcFile)) return bset;
  bset.add(srcFile);
  for (const dep of importMap.get(srcFile) ?? []) imports(dep, bset);
  return bset;
}

// fs.watch fires multiple times on some platforms, we must debounce per filename
function onChanges(themedSources: Set<string>, dir: string, eventType: string, srcFile: string): void {
  if (eventType === 'change') {
    srcFile = path.join(dir, srcFile);
    if (debounceMap.has(srcFile)) return;
    env.log(`Modified '${c.magenta(srcFile)}'`);
    debounceMap.set(
      srcFile,
      setTimeout(() => {
        sassBuild([...imports(srcFile)].filter(x => themedSources.has(x)));
        debounceMap.delete(srcFile);
      }, 200)
    );
  } else if (eventType === 'rename') {
    // TODO - a new or deleted scss file requires the sets/maps to be rebuilt and maybe
    // new themedSources to be generated, basically a full sass rebuild.  skip for now.
    env.log(`'${c.cyan(dir)}' ${c.error('Folder entries changed')} - relaunch bleep`);
  }
}

const importRe = /@import ['"](.*)['"]/g;

async function parseImports(src: string, depth = 1, processed = new Set<string>()) {
  if (depth > 10) {
    // arbitrary
    env.log(`${errorMark} '${c.cyan(src)}' - max depth exceeded (${depth})`);
    ps.exit(-2);
  }
  if (processed.has(src)) return;
  processed.add(src);

  try {
    for (const match of (await fs.promises.readFile(src, 'utf8')).matchAll(importRe)) {
      if (match.length !== 2) continue;
      const absDep = path.resolve(path.dirname(src), resolvePartial(match[1]));
      if (!absDep.startsWith(env.uiDir)) continue;
      const dep = absDep.slice(env.uiDir.length + 1);
      if (!importMap.get(dep)?.add(src)) importMap.set(dep, new Set<string>([src]));
      await parseImports(dep, depth + 1, processed);
    }
  } catch (e) {
    env.log(`${errorMark} failed to read ${src} - ${JSON.stringify(e, undefined, 2)}`);
  }
}

function resolvePartial(partial: string): string {
  const nameBegin = partial.lastIndexOf(path.sep) + 1;
  return `${partial.slice(0, nameBegin)}_${partial.slice(nameBegin)}.scss`;
}
