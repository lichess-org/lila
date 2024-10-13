import path from 'node:path';
import fs from 'node:fs';
import { XMLParser } from 'fast-xml-parser';
import { env, colors as c } from './main.ts';
import { globArray } from './parse.ts';
import { i18nManifest } from './manifest.ts';
import { quantize } from './build.ts';

type Plural = { [key in 'zero' | 'one' | 'two' | 'few' | 'many' | 'other']?: string };
type Dict = Map<string, string | Plural>;
type DictMap = Map<string, Map<string, string | Plural>>;

let locales: string[], dicts: string[];
let watchTimeout: NodeJS.Timeout | undefined;
const i18nWatch: fs.FSWatcher[] = [];
const isFormat = /%(?:[\d]\$)?s/;

const tsPrelude = `// Generated
interface I18nFormat {
  (...args: (string | number)[]): string;
  asArray: <T>(...args: T[]) => (T | string)[], // vdom
}
interface I18nPlural {
  (quantity: number, ...args: (string | number)[]): string, // pluralSame
  raw: (quantity: number, ...args: (string | number)[]) => string, // plural
  asArray: <T>(quantity: number, ...args: T[]) => (T | string)[], // vdomPlural
}
interface I18n {\n`;

const jsPrelude =
  '"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||""}function ' +
  'p(t){let r=(n,e)=>l(o(t,n),n,e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t)' +
  '{let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,...r){let ' +
  'n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else ' +
  'for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}';

export function stopI18n(): void {
  clearTimeout(watchTimeout);
  watchTimeout = undefined;
  for (const watcher of i18nWatch) watcher.close();
  i18nWatch.length = 0;
}

export async function i18n(): Promise<void> {
  if (!env.i18n) return;

  [locales, dicts] = (
    await Promise.all([
      globArray('*.xml', { cwd: path.join(env.i18nDestDir, 'site'), absolute: false }),
      globArray('*.xml', { cwd: env.i18nSrcDir, absolute: false }),
    ])
  ).map(list => list.map(x => x.split('.')[0]));

  await compileTypings();
  compileJavascripts(); // no await

  if (!env.watch) return;

  const onChange = () => {
    clearTimeout(watchTimeout);
    watchTimeout = setTimeout(() => compileTypings().then(() => compileJavascripts(false)), 2000);
  };
  i18nWatch.push(fs.watch(env.i18nSrcDir, onChange));
  for (const d of dicts) {
    i18nWatch.push(fs.watch(path.join(env.i18nDestDir, d), onChange));
  }
}

async function compileTypings(): Promise<void> {
  await fs.promises.mkdir(env.i18nJsDir).catch(() => {});
  const dictStats = await Promise.all(dicts.map(d => updated(d)));
  if (dictStats.some(x => x)) {
    env.log(`Building ${c.grey('i18n')}`);
    const dictMap = new Map<string, Dict>();
    await Promise.all(
      dicts.map(async d =>
        dictMap.set(d, parseXml(await fs.promises.readFile(path.join(env.i18nSrcDir, `${d}.xml`), 'utf8'))),
      ),
    );
    await writeTypescript(dictMap);
  }
}

async function compileJavascripts(dirty: boolean = true): Promise<void> {
  for (const dict of dicts) {
    await Promise.all(
      [undefined, ...locales].map(locale =>
        updated(dict, locale).then(async xstat => {
          if (!xstat) return;
          if (!dirty) env.log(`Building ${c.grey('i18n')}`);
          dirty = true;
          return writeJavascript(dict, locale, xstat);
        }),
      ),
    );
  }
  if (dirty) i18nManifest();
}

async function updated(dict: string, locale?: string): Promise<fs.Stats | false> {
  const xmlPath = locale
    ? path.join(env.i18nDestDir, dict, `${locale}.xml`)
    : path.join(env.i18nSrcDir, `${dict}.xml`);
  const jsPath = path.join(env.i18nJsDir, `${dict}.${locale ?? 'en-GB'}.js`);
  const [xml, js] = await Promise.allSettled([fs.promises.stat(xmlPath), fs.promises.stat(jsPath)]);
  return xml.status === 'rejected' ||
    (js.status !== 'rejected' && quantize(xml.value.mtimeMs, 2000) === quantize(js.value.mtimeMs, 2000))
    ? false
    : xml.value;
}

async function writeJavascript(dict: string, locale?: string, xstat: fs.Stats | false = false) {
  const translations = parseXml(
    await fs.promises.readFile(
      locale ? path.join(env.i18nDestDir, dict, `${locale}.xml`) : path.join(env.i18nSrcDir, `${dict}.xml`),
      'utf-8',
    ),
  );
  const code =
    jsPrelude +
    `if(!window.i18n)window.i18n={};` +
    `if(!window.i18n.${dict})window.i18n.${dict}={};` +
    `let i=window.i18n.${dict};` +
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
  const filename = path.join(env.i18nJsDir, `${dict}.${locale ?? 'en-GB'}.js`);
  await fs.promises.writeFile(filename, code);
  if (!xstat) return;
  return fs.promises.utimes(filename, xstat.mtime, xstat.mtime);
}

async function writeTypescript(dictMap: DictMap) {
  const code =
    tsPrelude +
    [...dictMap]
      .map(
        ([dict, trans]) =>
          `  ${dict}: {\n` +
          [...trans.entries()]
            .map(([k, v]) => {
              const tpe = typeof v !== 'string' ? 'I18nPlural' : isFormat.test(v) ? 'I18nFormat' : 'string';
              const comment = typeof v === 'string' ? v.split('\n')[0] : v['other']?.split('\n')[0];
              return `    /** ${comment} */\n    '${k}': ${tpe};`;
            })
            .join('\n') +
          '\n  };\n',
      )
      .join('') +
    '}\n';
  return fs.promises.writeFile(path.join(env.typesDir, 'lichess', `i18n.d.ts`), code);
}

function parseXml(xmlData: string): Map<string, string | Plural> {
  const i18nMap: Map<string, string | Plural> = new Map();
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '' });
  const { string: strings, plurals } = parser.parse(xmlData).resources;
  for (const item of strings ? (Array.isArray(strings) ? strings : [strings]) : [])
    i18nMap.set(item.name, item['#text']);
  for (const plural of plurals ? (Array.isArray(plurals) ? plurals : [plurals]) : []) {
    const group: Record<string, string> = {};
    for (const item of Array.isArray(plural.item) ? plural.item : [plural.item]) {
      group[item.quantity] = item['#text'];
    }
    i18nMap.set(plural.name, group);
  }
  return new Map([...i18nMap.entries()].sort(([a], [b]) => a.localeCompare(b)));
}
