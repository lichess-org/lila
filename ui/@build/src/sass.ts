import * as cps from 'node:child_process';
import * as fs from 'node:fs';
import * as ps from 'node:process';
import * as path from 'node:path';
import { env, colors as c, lines, errorMark } from './main';
import { buildModules } from './build';
import { globArray } from './parse';

export async function sass(): Promise<void> {
  if (!env.sass) return;

  builder.clear();
  importMap.clear();

  await buildThemedScss();

  const allSources = await globArray('./*/css/**/[^_]*.scss', { abs: false });

  const modNames = buildModules.map(x => x.name);

  // filter to modules we're actually building
  builder.sources = new Set<string>(
    allSources.filter(x => modNames.find(y => x.startsWith(`${y}${path.sep}`))),
  );

  for (const build of builder.sources) {
    await parseImports(build);
  }

  if (env.watch) {
    for (const dir of [...importMap.keys()].map(path.dirname)) {
      const watcher = fs.watch(dir);
      watcher.on('change', onChanges.bind(null, dir));
      watcher.on('error', (err: Error) => env.error(err, 'sass'));
      watcher.on('close', () => {
        env.error('Watcher closed unexpectedly. Exiting');
        ps.exit(-1);
      });
    }
  }
  if (builder.sources.size) {
    compile([...builder.sources], false);
  } else env.done(0, 'sass');
}

const sassArgs = ['--no-error-css', '--stop-on-error', '--no-color', '--quiet', '--quiet-deps'];
const importMap = new Map<string, Set<string>>(); // (cssFile, sourcesThatImportIt)
const partialRe = /(.*)\/_(.*)\.scss$/;
const importRe = /@import ['"](.*)['"]/g;
const builder = new (class {
  fileSet = new Set<string>();
  timeout: NodeJS.Timeout | undefined;
  sources: Set<string>;

  clear() {
    clearTimeout(this.timeout);
    this.timeout = undefined;
    this.fileSet.clear();
  }

  add(files: string[]): boolean {
    // filenames relative to lila/ui
    const oldCount = this.fileSet.size;
    files.forEach(src => imports(src).forEach(dest => this.fileSet.add(dest)));
    if (this.timeout) {
      clearTimeout(this.timeout);
    }
    this.timeout = setTimeout(() => this.fire(), 200);
    return this.fileSet.size > oldCount;
  }

  fire() {
    compile([...this.fileSet].filter(x => this.sources.has(x)));
    this.clear();
  }
})();

async function buildThemedScss() {
  env.log('Building themed scss', { ctx: 'sass' });

  const partials: string[] = (await globArray('./*/css/build/_*.scss', { abs: false })).filter(
    (f: string) => !f.endsWith('.abstract.scss'),
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
          continue;
        }
        const code =
          `@import '../../../common/css/dir/${direction}';\n` +
          `@import '../../../common/css/theme/${theme}';\n` +
          `@import '${partial}';\n`;
        try {
          await fs.promises.writeFile(themed, code);
        } catch (e) {
          env.log(e, { error: true });
        }
      }
    }
  }
}

function compile(sources: string[], tellTheWorld = true) {
  if (tellTheWorld) {
    for (const srcFile of sources) {
      env.log(`Building '${c.cyan(srcFile)}'`, { ctx: 'sass' });
    }
  } else {
    env.log(`Building css with ${ps.platform}-${ps.arch}/sass`, { ctx: 'sass' });
  }

  const sassExec = path.join(env.buildDir, 'dart-sass', `${ps.platform}-${ps.arch}`, 'sass');
  const proc = cps.spawn(
    sassExec,
    sassArgs.concat(
      env.prod ? ['--style=compressed', '--no-source-map'] : ['--embed-sources'],
      sources.map(
        (src: string) =>
          `${src}:${path.join(
            env.cssDir,
            path.basename(src).replace(/(.*)scss$/, env.prod ? '$1min.css' : '$1dev.css'),
          )}`,
      ),
    ),
  );

  proc.stdout?.on('data', (buf: Buffer) => {
    const txts = lines(buf.toString('utf8'));
    for (const txt of txts) env.log(c.red(txt), { ctx: 'sass' });
  });
  proc.stderr?.on('data', (buf: Buffer) => sassError(buf.toString('utf8')));
  proc.on('close', (code: number) => env.done(code, 'sass'));
}

function imports(srcFile: string, bset = new Set<string>()): Set<string> {
  if (bset.has(srcFile)) return bset;
  bset.add(srcFile);
  for (const dep of importMap.get(srcFile) ?? []) imports(dep, bset);
  return bset;
}

function onChanges(dir: string, eventType: string, srcFile: string): void {
  if (eventType === 'change') {
    if (builder.add([path.join(dir, srcFile)])) env.log(`File '${c.cyanBold(srcFile)}' changed`);
  } else if (eventType === 'rename') {
    globArray('*.scss', { cwd: dir, abs: false }).then(files => {
      if (builder.add(files.map(f => path.join(dir, f)))) {
        env.log(`Directory '${c.cyanBold(dir)}' changed`);
      }
    });
  }
}

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

const hrule = '---------------------------------------------------------------------------';

function sassError(error: string) {
  for (const err of lines(error)) {
    if (err.startsWith('Error:')) {
      env.log(c.grey(hrule), { ctx: 'sass' });
      env.log(`${errorMark} - ${err.slice(7)}`, { ctx: 'sass' });
    } else env.log(err, { ctx: 'sass' });
  }
}
