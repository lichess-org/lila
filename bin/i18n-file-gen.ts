import { readdirSync, readFileSync, writeFileSync } from 'fs';
import path from 'path';
import { XMLParser } from 'fast-xml-parser';
import { fileURLToPath } from 'url';

interface TranslationXml {
  '?xml': {
    version: string;
    encoding: string;
  };
  resources: {
    string?: {
      name: string;
      text: string;
      comment?: string;
    }[];
    plurals?: {
      name: string;
      text: string;
      comment?: string;
      item: {
        quantity: 'one' | 'other';
        text: string;
      }[];
    }[];
  };
}

interface KeyList {
  name: string;
  code: string;
}

const __filename = fileURLToPath(import.meta.url);
const lilaDir = path.resolve(path.dirname(__filename), '..');
const baseDir = path.resolve(lilaDir, 'translation/source');

const xmls = readdirSync(baseDir)
  .filter(f => f.endsWith('.xml'))
  .map(f => f.replace(/\.xml$/, ''));

function convertObjectName(name: string) {
  const renames = new Map([['class', 'clas']]);

  return renames.get(name) || name;
}

function keyListFrom(name: string): KeyList {
  const txt = readFileSync(path.resolve(baseDir, `${name}.xml`), 'utf-8');
  const parser = new XMLParser({
    ignoreAttributes: false,
    isArray: tagName => ['string', 'plurals', 'item'].includes(tagName),
    attributeNamePrefix: '',
    textNodeName: 'text',
  });
  const xml: TranslationXml = parser.parse(txt);

  const keys = [
    ...(xml.resources.string || []).map(e => e.name),
    ...(xml.resources.plurals || []).map(e => e.name),
  ];
  const indent = ' '.repeat(4);

  return {
    name,
    code:
      keys
        .map(k => `${indent}val \`${k}\`: I18nKey = "${name === 'site' ? '' : name + ':'}${k}"`)
        .join('\n') + '\n',
  };
}

const dbCode = (obj: KeyList) => `  object ${convertObjectName(obj.name)}:\n${obj.code}`;

Promise.all(xmls.map(keyListFrom)).then(objs => {
  const code = `// Generated with bin/i18n-file-gen.ts
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

  // format: OFF
${objs.map(dbCode).join('\n')}
`;

  const keyFile = path.resolve(lilaDir, 'modules/coreI18n/src/main/key.scala');
  writeFileSync(keyFile, code);

  console.log(`✅ Wrote ${keyFile}`);
});
