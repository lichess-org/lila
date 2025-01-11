import { readdir, writeFile } from 'node:fs/promises';
import * as path from 'node:path';
import { parseXmls } from './parser.js';
import { categoryName } from './util.js';

export async function dumpTypes(baseDir: string): Promise<void> {
  const sourceDir = path.join(baseDir, 'translation/source');
  const sourceFilenames = (await readdir(sourceDir))
    .filter(file => file.endsWith('.xml'))
    .sort((a, b) => (a === 'site.xml' ? -1 : b === 'site.xml' ? 1 : 0));

  const parsed = await parseXmls(
    sourceFilenames.map(sf => {
      return { name: categoryName(sf), path: path.join(sourceDir, sf) };
    }),
  );

  const codeTs = `// Generated with bin/trans-dump.ts

// biome-ignore format: Auto generated
export type I18nKey =
${Object.keys(parsed)
  .map(k => `  | '${k}'`)
  .join('\n')};\n`;

  await writeFile(path.join(baseDir, '/ui/i18n/src/i18n-keys.ts'), codeTs);
}
