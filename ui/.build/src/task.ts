import fg from 'fast-glob';
import mm from 'micromatch';
import fs from 'node:fs';
import p from 'node:path';
import { glob, isFolder, subfolders } from './parse.ts';
import { randomToken } from './algo.ts';
import { type Context, env, c, errorMark } from './env.ts';

const fsWatches = new Map<AbsPath, FSWatch>();
const tasks = new Map<TaskKey, Task>();
const fileTimes = new Map<AbsPath, number>();

type Path = string;
type AbsPath = string;
type CwdPath = { cwd: AbsPath; path: Path };
type Debounce = { time: number; timeout?: NodeJS.Timeout; rename: boolean; files: Set<AbsPath> };
type TaskKey = string;
type FSWatch = { watcher: fs.FSWatcher; cwd: AbsPath; keys: Set<TaskKey> };
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
  key?: TaskKey; // optional key for replace & cancel
  ctx?: Context; // optional context for logging
  pkg?: string; // optional package for logging
  debounce?: number; // optional number in ms
  root?: AbsPath; // optional relative root for file lists, otherwise all paths are absolute
  globListOnly?: boolean; // default false - ignore file mods, only execute when glob list changes
  monitorOnly?: boolean; // default false - do not execute on initial traverse, only on future changes
  noEnvStatus?: boolean; // default false - don't inform env.done of task status
};

export async function task(o: TaskOpts): Promise<void> {
  const { monitorOnly: noInitial, debounce, key: inKey } = o;
  const glob = Array<CwdPath>().concat(o.glob ?? []);
  if (glob.length === 0) return;
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
  if (!noInitial) return execute(newWatch);
}

export function stopTask(keys?: TaskKey | TaskKey[]) {
  const stopKeys = Array<TaskKey>().concat(keys ?? [...tasks.keys()]);
  for (const key of stopKeys) {
    clearTimeout(tasks.get(key)?.debounce.timeout);
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

export async function watchGlob({ cwd, path: globPath }: CwdPath, key: TaskKey): Promise<any> {
  if (!(await isFolder(cwd))) return;
  const [head, ...tail] = globPath.split(p.sep);
  const path = tail.join(p.sep);

  if (head.includes('**')) {
    await subfolders(cwd, 10).then(folders => folders.forEach(f => addFsWatch(f, key)));
  } else if (/[*?!{}[\]()]/.test(head) && path) {
    await subfolders(cwd, 1).then(folders =>
      Promise.all(
        folders.filter(f => mm.isMatch(p.basename(f), head)).map(f => watchGlob({ cwd: f, path }, key)),
      ),
    );
  } else if (path) {
    return watchGlob({ cwd: p.join(cwd, head), path }, key);
  }
  addFsWatch(cwd, key);
}

async function onFsChange(fsw: FSWatch, event: string, filename: string | null) {
  const fullpath = p.join(fsw.cwd, filename ?? '');

  if (event === 'change') fileTimes.set(fullpath, await cachedFileTime(fullpath, true));
  for (const watch of [...fsw.keys].map(k => tasks.get(k)!)) {
    const fullglobs = watch.glob.map(({ cwd, path }) => p.join(cwd, path));
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
    clearTimeout(watch.debounce.timeout);
    watch.debounce.timeout = setTimeout(() => execute(watch), watch.debounce.time);
  }
}

async function execute(watch: Task): Promise<void> {
  const relative = (files: AbsPath[]) => (watch.root ? files.map(f => p.relative(watch.root!, f)) : files);
  const debounced = Object.freeze([...watch.debounce.files]);
  const modified: AbsPath[] = [];
  watch.debounce.files.clear();

  if (watch.debounce.rename) {
    watch.debounce.rename = false;
    const files = await globTimes(watch.glob);
    const keys = [...files.keys()];
    if (
      watch.globListOnly &&
      (watch.fileTimes.size !== files.size || !keys.every(f => watch.fileTimes.has(f)))
    ) {
      modified.push(...keys);
    } else if (!watch.globListOnly) {
      for (const [fullpath, time] of [...files]) {
        if (watch.fileTimes.get(fullpath) !== time) modified.push(fullpath);
      }
    }
    watch.fileTimes = files;
  } else if (!watch.globListOnly) {
    await Promise.all(
      debounced.map(async file => {
        const fileTime = await cachedFileTime(file);
        if (watch.fileTimes.get(file) === fileTime) return;
        watch.fileTimes.set(file, fileTime);
        modified.push(file);
      }),
    );
  }
  if (!modified.length) return;

  if (watch.ctx) env.begin(watch.ctx);
  watch.status = undefined;
  try {
    await watch.execute(relative(modified), relative([...watch.fileTimes.keys()]));
    watch.status = 'ok';
    if (watch.ctx && !watch.noEnvStatus && taskOk(watch.ctx)) env.done(watch.ctx);
  } catch (e) {
    watch.status = 'error';
    const message = e instanceof Error ? (e.stack ?? e.message) : String(e);
    if (!env.watch) env.exit(`${errorMark} ${message}`, watch.ctx);
    else if (e)
      env.log(`${errorMark} ${watch.pkg ? `[${c.grey(watch.pkg)}] ` : ''}- ${c.grey(message)}`, watch.ctx);
    if (watch.ctx && !watch.noEnvStatus) env.done(watch.ctx, -1);
  }
}

function addFsWatch(root: AbsPath, key: TaskKey) {
  if (fsWatches.has(root)) {
    fsWatches.get(root)?.keys.add(key);
    return;
  }
  const fsWatch = { watcher: fs.watch(root), cwd: root, keys: new Set([key]) };
  fsWatch.watcher.on('change', (event, f) => onFsChange(fsWatch, event, String(f)));
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
