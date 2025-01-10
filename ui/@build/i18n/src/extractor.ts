import { promises as fs } from 'node:fs';
import fg from 'fast-glob';

const IMPORT_REGEX = /import\s*{([^}]+)}\s*from\s*['"]i18n(?:\/i18n)?['"`]/g;
const LOCAL_IMPORT_REGEX = /import\s*{([^}]+)}\s*from\s*['"](?:\.)(?:\/i18n)?['"`]/g;

async function extractI18nKeys(packageName: string, filePath: string): Promise<Set<string>> {
  const keys = new Set<string>(),
    importedFunctions = new Set<string>();

  try {
    const sourceCode = await fs.readFile(filePath, 'utf-8'),
      reg = packageName === 'i18n' ? LOCAL_IMPORT_REGEX : IMPORT_REGEX;

    for (const match of sourceCode.matchAll(reg)) {
      const imports = match[1].split(',');
      for (const imp of imports) {
        const parts = imp.trim().split(/\s+as\s+/),
          functionName = parts[parts.length - 1].trim();
        importedFunctions.add(functionName);
      }
    }

    const functionPattern = Array.from(importedFunctions).join('|');
    if (!functionPattern) return keys;

    const callRegex = new RegExp(
      `(?<![.])(?:\\.{3})?\\b(?:${functionPattern})\\s*\\(\\s*(['"\`])([^\\'"\`]+)\\1`,
      'g',
    );
    for (const match of sourceCode.matchAll(callRegex)) {
      keys.add(match[2]);
    }
  } catch (error) {
    console.error(`Error processing file ${filePath}:`, error);
  }

  return keys;
}

export async function extractI18nKeysFromDir(
  packageName: string,
  dirPath: string,
): Promise<Set<string>> {
  const allKeys = new Set<string>(),
    files = await fg(['**/*.{ts,js}'], {
      cwd: dirPath,
      absolute: true,
      ignore: ['**/node_modules/**'],
    });

  const fileKeysArr = await Promise.all(files.map(file => extractI18nKeys(packageName, file)));

  for (const fileKeys of fileKeysArr) {
    for (const key of fileKeys) {
      allKeys.add(key);
    }
  }

  return allKeys;
}
