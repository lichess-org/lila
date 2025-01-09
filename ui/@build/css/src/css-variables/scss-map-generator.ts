import fs from 'fs';
import path from 'path';
import { signature } from './constants.js';

export async function generateScssMap(
  themes: Record<string, Record<string, string>>,
  extracted: Set<string>,
  outDir: string,
): Promise<void> {
  let output = `${signature}\n`;

  const themeKeys = new Set();
  Object.values(themes).forEach(theme => {
    Object.keys(theme).forEach(k => themeKeys.add(k));
  });

  for (const k of Array.from(themeKeys).sort()) {
    output += `$${k}: var(--${k});\n`;
  }

  Array.from(extracted)
    .sort()
    .forEach(name => {
      output += `$${name}: var(--${name});\n`;
    });

  await fs.promises.writeFile(path.join(outDir, '_theme.scss'), output);
}
