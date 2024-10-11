import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { XMLParser } from 'fast-xml-parser';

type Plural = { [key in 'zero' | 'one' | 'two' | 'few' | 'many' | 'other']?: string };
type DictMap = Map<string, Map<string, string | Plural>>;
type I18nMap = Map<string, DictMap>;

const lilaDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const sourceDir = path.join(lilaDir, 'translation', 'source');
const destDir = path.join(lilaDir, 'translation', 'dest');
const srcDir = path.join(lilaDir, 'translation', 'js');
const isFormat = /%(?:[\d]\$)?s/;
const scalaKeysPrelude = `// Generated
package lila.core.i18n

opaque type I18nKey = String
object I18nKey:
  def apply(key: String): I18nKey = key
  import scalatags.Text.RawFrag
  extension (key: I18nKey)
    def value: String = key
    def txt(args: Any*)(using trans: Translate): String =
      trans.translator.txt.literal(key, args, trans.lang)
    def pluralTxt(count: Count, args: Any*)(using trans: Translate): String =
      trans.translator.txt.plural(key, count, args, trans.lang)
    def pluralSameTxt(count: Long)(using trans: Translate): String = pluralTxt(count, count)
    def apply(args: Matchable*)(using trans: Translate): RawFrag =
      trans.translator.frag.literal(key, args, trans.lang)
    def plural(count: Count, args: Matchable*)(using trans: Translate): RawFrag =
      trans.translator.frag.plural(key, count, args, trans.lang)
    def pluralSame(count: Int)(using trans: Translate): RawFrag = plural(count, count)

  // format: OFF`;

const tsPrelude = `// Generated
interface I18nFormat {
  (args: (string | number)[]): string;
  asArray: <T>(args: T[]) => (T | string)[],
}
interface I18nPlural {
  (quantity: number, args: (string | number)[]): string,
  asArray: <T>(quantity: number, args: T[]) => (T | string)[],
}
interface I18n {\n`;

const jsPrelude =
  '"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural ' +
  'found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}' +
  'function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){' +
  'let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];' +
  'else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}';

const i18nMap = await readTranslations();

await writeScalaKeys(i18nMap.get('en-GB')!);
await writeTypescript(i18nMap.get('en-GB')!);
await writeJavascripts(i18nMap);

async function readTranslations() {
  const [dicts, locales] = (
    await Promise.all([fs.promises.readdir(sourceDir), fs.promises.readdir(path.join(destDir, 'site'))])
  ).map(files => files.filter(xml => xml.endsWith('.xml')).map(filename => filename.split('.')[0]));
  const i18nMap: I18nMap = new Map();
  for (const dict of dicts) {
    const en = i18nMap.get('en-GB') ?? new Map();
    en.set(dict, parseXml(await fs.promises.readFile(path.join(sourceDir, `${dict}.xml`), 'utf8')));
    i18nMap.set('en-GB', en);
    for (const locale of locales) {
      const dictMap: DictMap = i18nMap.get(locale) ?? new Map();
      if (!fs.existsSync(path.join(destDir, dict, `${locale}.xml`))) continue;
      dictMap.set(
        dict,
        parseXml(await fs.promises.readFile(path.join(destDir, dict, `${locale}.xml`), 'utf8')),
      );
      i18nMap.set(locale, dictMap);
    }
  }
  return i18nMap;
}

async function writeScalaKeys(dictMap: DictMap) {
  const code =
    scalaKeysPrelude +
    [...dictMap]
      .map(
        ([dict, trans]) =>
          `\n  object ${dict === 'class' ? 'clas' : dict}:\n` +
          [...trans.keys()]
            .map(k => `    val \`${k}\`: I18nKey = "${dict === 'site' ? '' : dict + ':'}${k}"\n`)
            .join(''),
      )
      .join('');
  return writeIfChanged(path.join(lilaDir, 'modules', 'coreI18n', 'src', 'main', 'key.scala'), code);
}

async function writeTypescript(dictMap: DictMap) {
  const code =
    tsPrelude +
    [...dictMap]
      .map(
        ([dict, trans]) =>
          `  ${dict}: {\n` +
          [...trans.entries()]
            .map(
              ([k, v]) =>
                `    '${k}': ` +
                (typeof v !== 'string' ? 'I18nPlural' : isFormat.test(v) ? 'I18nFormat' : 'string'),
            )
            .join(';\n') +
          ';\n  };\n',
      )
      .join('') +
    '}\n';
  return writeIfChanged(path.join(lilaDir, 'ui', '@types', 'lichess', `i18n.d.ts`), code);
}

async function writeJavascripts(i18nMap: I18nMap) {
  await fs.promises.mkdir(srcDir, { recursive: true });
  for (const [locale, dictMap] of i18nMap) {
    for (const [dict, translations] of dictMap) {
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
      await writeIfChanged(path.join(srcDir, `${dict}.${locale}.js`), code);
    }
  }
}

function parseXml(xmlData: string): Map<string, string | Plural> {
  const i18nMap: Map<string, string | Plural> = new Map();
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '' });
  const { string: strings, plurals } = parser.parse(xmlData).resources;
  for (const item of strings ? (Array.isArray(strings) ? strings : [strings]) : [])
    i18nMap.set(item.name, item['#text']);
  for (const plural of plurals ? (Array.isArray(plurals) ? plurals : [plurals]) : []) {
    const group = {};
    for (const item of Array.isArray(plural.item) ? plural.item : [plural.item]) {
      group[item.quantity] = item['#text'];
    }
    i18nMap.set(plural.name, group);
  }
  return new Map([...i18nMap.entries()].sort(([a], [b]) => a.localeCompare(b)));
}

async function writeIfChanged(filename: string, content: string): Promise<void> {
  if (fs.existsSync(filename) && (await fs.promises.readFile(filename, 'utf8')) === content) return;
  return fs.promises.writeFile(filename, content); // preserve mod dates
}
