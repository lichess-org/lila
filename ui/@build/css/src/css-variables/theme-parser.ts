import * as fs from 'node:fs';
import path from 'node:path';
import { themes } from './constants.js';
import type { ThemeRecord } from './types.js';

export async function parseThemes(themeDir: string): Promise<ThemeRecord> {
  const themeRec = {} as ThemeRecord;

  await Promise.all(
    themes.map(async t => {
      themeRec[t] = await parseThemeFile(path.join(themeDir, `_${t}.scss`));
    }),
  );

  return themeRec;
}

async function parseThemeFile(filePath: string): Promise<Record<string, string>> {
  const content = await fs.promises.readFile(filePath, 'utf-8'),
    regex = /^\$([\w-_]+):\s*([^;]+);/gm,
    variables: Record<string, string> = {};

  for (const match of content.matchAll(regex)) {
    const [, name, value] = match;
    if (name && value && !name.startsWith('_')) {
      const trimmed = value.replaceAll('\n', '').trim();
      variables[name] = trimmed;
    }
  }

  return variables;
}
