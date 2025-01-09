import * as fs from 'fs';
import { prefix } from './constants.js';

export async function extractVariables(files: string[]): Promise<Set<string>> {
  const found = new Set<string>();

  for (const file of files.filter(f => !f.includes('theme/gen'))) {
    const content = await fs.promises.readFile(file, 'utf-8'),
      regex = new RegExp(`\\$${prefix}[\\w-]{2,}_(?:neg)?\\d+`, 'g'),
      matches = content.match(regex) || [];

    matches.forEach(match => {
      found.add(match.slice(1));
    });
  }

  return found;
}
