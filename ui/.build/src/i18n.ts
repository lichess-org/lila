import path from 'node:path';
import fs from 'node:fs';
import { XMLParser } from 'fast-xml-parser';
import { env, colors as c } from './main.ts';
import { globArray } from './parse.ts';
import { i18nManifest } from './manifest.ts';
import { quantize } from './build.ts';
import { transform } from 'esbuild';

type Plural = { [key in 'zero' | 'one' | 'two' | 'few' | 'many' | 'other']?: string };
type Dict = Map<string, string | Plural>;

let dicts: Map<string, Dict> = new Map();
let locales: string[], cats: string[];
let watchTimeout: NodeJS.Timeout | undefined;
const i18nWatch: fs.FSWatcher[] = [];
const isFormat = /%(?:[\d]\$)?s/;

const tsPrelude = `// Generated
interface I18nFormat {
  (...args: (string | number)[]): string; // formatted
  asArray: <T>(...args: T[]) => (T | string)[]; // vdom
}
interface I18nPlural {
  (quantity: number, ...args: (string | number)[]): string; // pluralSame
  asArray: <T>(quantity: number, ...args: T[]) => (T | string)[]; // vdomPlural / plural
}
interface I18n {
  /** Global noarg key lookup (only if absolutely necessary). */
  (key: string): string;\n\n`;

const jsPrelude =
  '"use strict";(()=>{' +
  (
    await transform(
      // s(...) is the standard format function, p(...) is the plural format function.
      // both have an asArray method for vdom.
      `function p(t) {
        let r = (n, ...e) => l(o(t, n), n, ...e).join('');
        return (r.asArray = (n, ...e) => l(o(t, n), ...e)), r;
      }
      function s(t) {
        let r = (...n) => l(t, ...n).join('');
        return (r.asArray = (...n) => l(t, ...n)), r;
      }
      function o(t, n) {
        return t[site.quantity(n)] || t.other || t.one || '';
      }
      function l(t, ...r) {
        let n = t.split(/(%(?:\\d\\$)?s)/);
        if (r.length) {
          let e = n.indexOf('%s');
          if (e !== -1) n[e] = r[0];
          else
            for (let i = 0; i < r.length; i++) {
              let s = n.indexOf('%' + (i + 1) + '$s');
              s !== -1 && (n[s] = r[i]);
            }
        }
        return n;
      }`,
      { minify: true, loader: 'js' },
    )
  ).code;

export function stopI18n(): void {
  clearTimeout(watchTimeout);
  watchTimeout = undefined;
  for (const watcher of i18nWatch) watcher.close();
  i18nWatch.length = 0;
}

export async function i18n(isBoot = true): Promise<void> {
  if (!env.i18n) return;

  [locales, cats] = (
    await Promise.all([
      globArray('*.xml', { cwd: path.join(env.i18nDestDir, 'site'), absolute: false }),
      globArray('*.xml', { cwd: env.i18nSrcDir, absolute: false }),
    ])
  ).map(list => list.map(x => x.split('.')[0]));

  await compileTypings();
  compileJavascripts(isBoot); // no await

  if (!isBoot || !env.watch) return;

  const onChange = () => {
    clearTimeout(watchTimeout);
    watchTimeout = setTimeout(() => i18n(false), 2000);
  };
  i18nWatch.push(fs.watch(env.i18nSrcDir, onChange));
  for (const d of cats) {
    await fs.promises.mkdir(path.join(env.i18nDestDir, d)).catch(() => {});
    i18nWatch.push(fs.watch(path.join(env.i18nDestDir, d), onChange));
  }
}

async function compileTypings(): Promise<void> {
  const typingsPathname = path.join(env.typesDir, 'lichess', `i18n.d.ts`);
  const [tstat] = await Promise.all([
    fs.promises.stat(typingsPathname).catch(() => undefined),
    fs.promises.mkdir(env.i18nJsDir).catch(() => {}),
  ]);
  const catStats = await Promise.all(cats.map(d => updated(d)));

  if (!tstat || catStats.some(x => x)) {
    env.log(`Building ${c.grey('i18n')}`);
    dicts = new Map(
      zip(
        cats,
        await Promise.all(
          cats.map(d => fs.promises.readFile(path.join(env.i18nSrcDir, `${d}.xml`), 'utf8').then(parseXml)),
        ),
      ),
    );
    await fs.promises.writeFile(
      typingsPathname,
      tsPrelude +
        [...dicts]
          .map(
            ([cat, dict]) =>
              `  ${cat}: {\n` +
              [...dict.entries()]
                .map(([k, v]) => {
                  if (!/^[A-Za-z_]\w*$/.test(k)) k = `'${k}'`;
                  const tpe =
                    typeof v !== 'string' ? 'I18nPlural' : isFormat.test(v) ? 'I18nFormat' : 'string';
                  const comment = typeof v === 'string' ? v.split('\n')[0] : v['other']?.split('\n')[0];
                  return `    /** ${comment} */\n    ${k}: ${tpe};`;
                })
                .join('\n') +
              '\n  };\n',
          )
          .join('') +
        '}\n',
    );
    const mstat = catStats.reduce(
      (a, b) => (a && b && quantize(a.mtimeMs) > quantize(b.mtimeMs) ? a : b),
      tstat || false,
    );
    if (mstat) await fs.promises.utimes(typingsPathname, mstat.mtime, mstat.mtime);
  }
}

async function compileJavascripts(dirty: boolean = true): Promise<void> {
  for (const cat of cats) {
    const u = await updated(cat);
    if (u) await writeJavascript(cat, undefined, u);
    await Promise.all(
      locales.map(locale =>
        updated(cat, locale).then(xstat => {
          if (!u && !xstat) return;
          if (!dirty) env.log(`Building ${c.grey('i18n')}`);
          dirty = true;
          return writeJavascript(cat, locale, xstat);
        }),
      ),
    );
  }
  if (dirty) i18nManifest();
}

async function writeJavascript(cat: string, locale?: string, xstat: fs.Stats | false = false) {
  if (!dicts.has(cat))
    dicts.set(
      cat,
      await fs.promises.readFile(path.join(env.i18nSrcDir, `${cat}.xml`), 'utf8').then(parseXml),
    );

  const translations = new Map([
    ...dicts.get(cat)!,
    ...(locale
      ? await fs.promises
          .readFile(path.join(env.i18nDestDir, cat, `${locale}.xml`), 'utf-8')
          .catch(() => '')
          .then(parseXml)
      : []),
  ]);
  const jsInit =
    cat === 'site'
      ? 'window.i18n=function(k){for(let v of Object.values(window.i18n))if(v[k])return v[k];return k};'
      : '';
  const code =
    jsPrelude +
    jsInit +
    `if(!window.i18n.${cat})window.i18n.${cat}={};` +
    `let i=window.i18n.${cat};` +
    [...translations]
      .map(
        ([k, v]) =>
          `i['${k}']=` +
          (typeof v !== 'string'
            ? `p(${JSON.stringify(v)})`
            : isFormat.test(v)
              ? `s(${JSON.stringify(v)})`
              : JSON.stringify(v)),
      )
      .join(';') +
    '})()';

  const filename = path.join(env.i18nJsDir, `${cat}.${locale ?? 'en-GB'}.js`);
  await fs.promises.writeFile(filename, code);

  if (!xstat) return;
  return fs.promises.utimes(filename, xstat.mtime, xstat.mtime);
}

async function updated(cat: string, locale?: string): Promise<fs.Stats | false> {
  const xmlPath = locale
    ? path.join(env.i18nDestDir, cat, `${locale}.xml`)
    : path.join(env.i18nSrcDir, `${cat}.xml`);
  const jsPath = path.join(env.i18nJsDir, `${cat}.${locale ?? 'en-GB'}.js`);
  const [xml, js] = await Promise.allSettled([fs.promises.stat(xmlPath), fs.promises.stat(jsPath)]);
  return xml.status === 'rejected' ||
    (js.status !== 'rejected' && quantize(xml.value.mtimeMs) <= quantize(js.value.mtimeMs))
    ? false
    : xml.value;
}

function parseXml(xmlData: string): Map<string, string | Plural> {
  const i18nMap: Map<string, string | Plural> = new Map();
  if (!xmlData) return i18nMap;

  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '' });
  const { string: strings, plurals } = parser.parse(xmlData).resources;
  for (const item of strings ? (Array.isArray(strings) ? strings : [strings]) : [])
    i18nMap.set(item.name, item['#text'].replaceAll('\\"', '"').replaceAll("\\'", "'"));
  for (const plural of plurals ? (Array.isArray(plurals) ? plurals : [plurals]) : []) {
    const group: Record<string, string> = {};
    for (const item of Array.isArray(plural.item) ? plural.item : [plural.item]) {
      group[item.quantity] = item['#text'].replaceAll('\\"', '"').replaceAll("\\'", "'");
    }
    i18nMap.set(plural.name, group);
  }
  return new Map([...i18nMap.entries()].sort(([a], [b]) => a.localeCompare(b)));
}

function zip<T, U>(arr1: T[], arr2: U[]): [T, U][] {
  const length = Math.min(arr1.length, arr2.length);
  const result: [T, U][] = [];
  for (let i = 0; i < length; i++) {
    result.push([arr1[i], arr2[i]]);
  }
  return result;
}
