#!/usr/bin/env node

import { execSync } from 'node:child_process';
import { readFile, readdir, writeFile } from 'node:fs/promises';
import * as path from 'node:path';
import { parseStringPromise } from 'xml2js';

interface ParsedXmlElement {
  name: string;
  keys: string[];
}
type ParsedXml = ParsedXmlElement[];

const baseFolder = path.dirname(execSync('pnpm root -w').toString());
const translationFolder = path.join(baseFolder, 'translation');
const sourceFolder = path.join(translationFolder, 'source');

function validName(name: string): string {
  const noExt = name.replace('.xml', '');
  return noExt === 'class' ? 'clas' : noExt;
}
async function writeScala(parsed: ParsedXml): Promise<void> {
  function mapKeys(el: ParsedXmlElement, nested = false): string {
    const spaces = nested ? '    ' : '  ';
    return `${el.keys
      .map(
        k =>
          `${spaces}val \`${k}\` = new I18nKey("${el.name === 'site' ? '' : `${el.name}:`}${k}")`,
      )
      .join('\n')}\n`;
  }
  function dbCode(el: ParsedXmlElement): string {
    return el.name === 'site'
      ? mapKeys(el)
      : `  object ${validName(el.name)} {\n${mapKeys(el, true)}}\n`;
  }
  const codeScala = `// Generated with bin/trans-dump.ts
package lila.i18n
// format: OFF
object I18nKeys {
${parsed.map(dbCode).join('\n')}
}`;

  await writeFile(path.join(baseFolder, 'modules/i18n/src/main/I18nKeys.scala'), codeScala);
}

try {
  const sourceFilenames = (await readdir(sourceFolder))
    .filter(file => file.endsWith('.xml'))
    .sort((a, b) => (a === 'site.xml' ? -1 : b === 'site.xml' ? 1 : 0));

  const parsedXml: ParsedXml = await Promise.all(
    sourceFilenames.map(async fileName => {
      const fileContent = await readFile(path.join(sourceFolder, fileName), { encoding: 'utf8' });
      const parsed = await parseStringPromise(fileContent);
      const strings = (parsed?.resources?.string || []).map((e: any) => e.$.name);
      const plurals = (parsed?.resources?.plurals || []).map((e: any) => e.$.name);
      const keys = [...strings, ...plurals];

      return {
        name: fileName.replace('.xml', ''),
        keys,
      };
    }),
  );
  await writeScala(parsedXml);
} catch (e) {
  console.error('Error: ', e);
  process.exit(1);
}
