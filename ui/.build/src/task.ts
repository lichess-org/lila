import fg from 'fast-glob';
import mm from 'micromatch';
import fs from 'node:fs';
import { join, relative, basename } from 'node:path';
import { type Package, glob, isFolder, subfolders, isClose } from './parse.ts';
import { randomToken } from './algo.ts';
import { type Context, env, c, errorMark } from './env.ts';

const fsWatches = new Map<AbsPath, FSWatch>();
const tasks = new Map<TaskKey, Task>();
const fileTimes = new Map<AbsPath, number>();

type Path = string;
type AbsPath = string;
type CwdPath = { cwd: AbsPath; path: Path };
type TaskKey = string;
type FSWatch = { watcher: fs.FSWatcher; cwd: AbsPath; keys: Set<TaskKey> };
type Debounce = {
  time: number;
  timer?: NodeJS.Timeout;
  rename: boolean;
  files: Set<AbsPath>;
};
type Task = Omit<TaskOpts, 'glob' | 'debounce'> & {
  glob: CwdPath[];
  key: TaskKey;
  debounce: Debounce;
  fileTimes: Map<AbsPath, number>;
  status: 'ok' | 'error' | undefined;
};
type TaskOpts = {
  glob: CwdPath | CwdPath[];
  execute: (touched: AbsPath[], fullList: AbsPath[]) => Promise<any>;
  key?: TaskKey; // optional key for overwrite, stop, tickle
  ctx?: Context; // optional build step context for logging
  pkg?: Package; // optional package reference
  debounce?: number; // optional number in ms
  root?: AbsPath; // default absolute - optional relative root for file lists
  globListOnly?: boolean; // default false - ignore file mods, only execute when glob list changes
  monitorOnly?: boolean; // default false - do not execute on initial traverse, only on future changes
  noEnvStatus?: boolean; // default false - don't inform env.done of task status
};

export async function task(o: TaskOpts): Promise<TaskKey> {
  const { monitorOnly: noInitial, debounce, key: inKey } = o;
  const glob = Array<CwdPath>().concat(o.glob ?? []);
  if (inKey) stopTask(inKey);
  const newWatch: Task = {
    ...o,
    glob,
    key: inKey ?? randomToken(),
    status: noInitial ? 'ok' : undefined,
    debounce: { time: debounce ?? 0, rename: !noInitial, files: new Set<AbsPath>() },
    fileTimes: noInitial ? await globTimes(glob) : new Map(),
  };
  tasks.set(newWatch.key, newWatch);
  if (env.watch) newWatch.glob.forEach(g => watchGlob(g, newWatch.key));
  if (!noInitial) await execute(newWatch);
  return newWatch.key;
}

export function stopTask(keys?: TaskKey | TaskKey[]) {
  const stopKeys = Array<TaskKey>().concat(keys ?? [...tasks.keys()]);
  for (const key of stopKeys) {
    clearTimeout(tasks.get(key)?.debounce.timer);
    tasks.delete(key);
    for (const [folder, fw] of fsWatches) {
      if (fw.keys.delete(key) && fw.keys.size === 0) {
        fw.watcher.close();
        fsWatches.delete(folder);
      }
    }
  }
}

export function taskOk(ctx?: Context): boolean {
  const all = [...tasks.values()].filter(w => (ctx ? w.ctx === ctx : true));
  return all.filter(w => !w.monitorOnly).length > 0 && all.every(w => w.status === 'ok');
}

async function execute(t: Task): Promise<void> {
  const makeRelative = (files: AbsPath[]) => (t.root ? files.map(f => relative(t.root!, f)) : files);
  const debounced = [...t.debounce.files];
  const modified: AbsPath[] = [];
  const { rename } = t.debounce;
  t.debounce.rename = false;
  t.debounce.files.clear();

  if (rename) {
    const files = await globTimes(t.glob);
    const keys = [...files.keys()];
    if (t.globListOnly && !(t.fileTimes.size === files.size && keys.every(f => t.fileTimes.has(f)))) {
      modified.push(...keys);
    } else if (!t.globListOnly) {
      for (const [fullpath, time] of [...files]) {
        if (!isClose(t.fileTimes.get(fullpath), time)) modified.push(fullpath);
      }
    }
    t.fileTimes = files;
  } else if (!t.globListOnly) {
    await Promise.all(
      debounced.map(async file => {
        const fileTime = await cachedFileTime(file);
        if (isClose(t.fileTimes.get(file), fileTime)) return;
        t.fileTimes.set(file, fileTime);
        modified.push(file);
      }),
    );
  }
  if (modified.length === 0) return;

  if (t.ctx) env.begin(t.ctx);
  t.status = undefined;
  try {
    await t.execute(makeRelative(modified), makeRelative([...t.fileTimes.keys()]));
    t.status = 'ok';
    if (t.ctx && !t.noEnvStatus && taskOk(t.ctx)) env.done(t.ctx);
  } catch (e) {
    t.status = 'error';
    const message = e instanceof Error ? (e.stack ?? e.message) : String(e);
    if (!env.watch) env.exit(`${errorMark} ${message}`, t.ctx);
    else if (e)
      env.log(`${errorMark} ${t.pkg?.name ? `[${c.grey(t.pkg.name)}] ` : ''}- ${c.grey(message)}`, t.ctx);
    if (t.ctx && !t.noEnvStatus) env.done(t.ctx, -1);
  }
}

async function watchGlob({ cwd, path: globPath }: CwdPath, key: TaskKey): Promise<any> {
  if (!(await isFolder(cwd))) return;
  const [head, ...tail] = globPath.split('/');
  const path = tail.join('/');

  if (head.includes('**')) {
    await subfolders(cwd, 10).then(folders => folders.forEach(f => addFsWatch(f, key)));
  } else if (/[*?!{}[\]()]/.test(head) && path) {
    await subfolders(cwd, 1).then(folders =>
      Promise.all(
        folders.filter(f => mm.isMatch(basename(f), head)).map(f => watchGlob({ cwd: f, path }, key)),
      ),
    );
  } else if (path) {
    return watchGlob({ cwd: join(cwd, head), path }, key);
  }
  addFsWatch(cwd, key);
}

async function onFsEvent(fsw: FSWatch, event: string, filename: string | null) {
  const fullpath = join(fsw.cwd, filename ?? '');

  if (event === 'change')
    try {
      await cachedFileTime(fullpath, true);
    } catch {
      fileTimes.delete(fullpath);
      event = 'rename';
    }
  for (const watch of [...fsw.keys].map(k => tasks.get(k)!)) {
    const fullglobs = watch.glob.map(({ cwd, path }) => join(cwd, path));
    if (!mm.isMatch(fullpath, fullglobs)) {
      if (event === 'change') continue;
      try {
        if (!(await fs.promises.stat(fullpath)).isDirectory()) continue;
      } catch {
        fsWatches.get(fullpath)?.watcher.close();
        fsWatches.delete(fullpath);
        continue;
      }
      addFsWatch(fullpath, watch.key);
      if (!(await glob(fullglobs, { cwd: undefined, absolute: true })).some(x => x.includes(fullpath)))
        continue;
    }
    if (event === 'rename') watch.debounce.rename = true;
    if (event === 'change') watch.debounce.files.add(fullpath);
    clearTimeout(watch.debounce.timer);
    watch.debounce.timer = setTimeout(() => execute(watch), watch.debounce.time);
  }
}

function addFsWatch(root: AbsPath, key: TaskKey) {
  if (fsWatches.has(root)) {
    fsWatches.get(root)?.keys.add(key);
    return;
  }
  const fsWatch = { watcher: fs.watch(root), cwd: root, keys: new Set([key]) };
  fsWatch.watcher.on('change', (event, f) => onFsEvent(fsWatch, event, String(f)));
  fsWatches.set(root, fsWatch);
}

async function cachedFileTime(file: AbsPath, update = false): Promise<number> {
  if (fileTimes.has(file) && !update) return fileTimes.get(file)!;
  const stat = (await fs.promises.stat(file)).mtimeMs;
  fileTimes.set(file, stat);
  return stat;
}

async function globTimes(paths: CwdPath[]): Promise<Map<AbsPath, number>> {
  const globs = paths.map(({ path, cwd }) => fg.glob(path, { cwd, absolute: true }));
  return new Map(
    await Promise.all(
      (await Promise.all(globs)).flat().map(async f => [f, await cachedFileTime(f)] as [string, number]),
    ),
  );
}
