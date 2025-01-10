import path from 'path';
import { mkdir } from 'fs/promises';
import { generateCssVariables } from './css-variable-generator.js';
import { generateScssMap } from './scss-map-generator.js';
import { parseThemes } from './theme-parser.js';

export async function build(extracted: Set<string>, themeDir: string): Promise<void> {
  try {
    const outDir = path.join(themeDir, 'gen'),
      themeVars = await parseThemes(themeDir);

    await mkdir(outDir, { recursive: true });

    await generateScssMap(themeVars, extracted, outDir);

    await generateCssVariables(themeVars, extracted, outDir);

    console.log('CSS variables build completed successfully!');
  } catch (error) {
    console.error('Build failed:', error);
    process.exit(1);
  }
}
