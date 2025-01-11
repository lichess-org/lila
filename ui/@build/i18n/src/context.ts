import fs from 'node:fs';
import { readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import type { Context, PackageInfo, PackagesWrap } from '@build/wrapper/types';
import { defaultLang, otherLangs } from './constants.js';
import { dumpTypes } from './dumper.js';
import { extractI18nKeysFromDir } from './extractor.js';
import { parseXmls } from './parser.js';
import { quantity } from './quantity.js';
import { timeago } from './timeago.js';
import type { I18nObj } from './types.js';
import { categoryName, isValidJs } from './util.js';

const separatePackages = ['learn', 'insights', 'puzzle'];

export function i18nContext(): Context {
  let outdir: string;
  let translationDir: string;
  let translationSourceDir: string;

  const packagesKeyCache: Record<string, Set<string>> = {};

  let sourceI18n: I18nObj;
  const langsI18n: Record<string, I18nObj> = {};

  let usedFilter: boolean;

  let rootPath: string;
  let packages: PackageInfo[];

  return {
    name: 'I18n',

    async init(pWrap: PackagesWrap, flags: string[]): Promise<void> {
      rootPath = pWrap.root.path;
      packages = pWrap.packages;

      usedFilter = flags.includes('--filter');

      if (usedFilter) console.log('--filter flag was used, output will not be correct.');

      translationDir = path.join(rootPath, 'translation');
      translationSourceDir = path.join(translationDir, 'source');
      outdir = path.join(rootPath, '/public/translation/');

      await Promise.all([initDirs(), initSources(), initLangDests(), initCache(), writeKeyTypes()]);
    },

    packageWatch: [{ path: 'src', options: { recursive: true } }],

    async run(pkg: PackageInfo) {
      console.log('Building i18n files...');
      packagesKeyCache[pkg.name] = await extractI18nKeysFromDir(
        pkg.name,
        path.join(pkg.path, 'src'),
      );
      await writeI18n();
    },

    async runAll() {
      console.log('Building i18n files...');
      await writeI18n();
    },

    globalWatch: [{ path: 'translation', options: { recursive: true } }],

    async global(_event: fs.WatchEventType, filepath: string): Promise<void> {
      const reread = filepath.includes('source/') ? initSources : initLangDests;
      await Promise.all([writeKeyTypes(), reread()]);
    },

    async stop(): Promise<void> {},
  };

  async function initDirs(): Promise<void> {
    await Promise.all(
      [...separatePackages, 'core'].map(async p =>
        fs.promises.mkdir(path.join(outdir, p), { recursive: true }),
      ),
    );
  }

  async function writeKeyTypes(): Promise<void> {
    if (!usedFilter) {
      dumpTypes(rootPath);
      console.log('Type file written.\n');
    } else console.warn('Skipping writing types because --filter flag was used');
  }

  async function initCache(): Promise<void> {
    const result = await Promise.all(
      packages.map(async pkg => {
        return {
          name: pkg.name,
          keys: await extractI18nKeysFromDir(pkg.name, path.join(pkg.path, 'src')),
        };
      }),
    );
    result.forEach(({ name, keys }) => {
      console.log(`${name}: ${keys.size} keys`);
      packagesKeyCache[name] = keys;
    });
  }

  async function initSources(): Promise<void> {
    const sourceFiles = (await readdir(translationSourceDir))
      .filter(file => file.endsWith('.xml'))
      .sort((a, b) => (a === 'site.xml' ? -1 : b === 'site.xml' ? 1 : 0));
    sourceI18n = await parseXmls(
      sourceFiles.map(sf => {
        return { name: categoryName(sf), path: path.join(translationSourceDir, sf) };
      }),
    );
  }

  async function initLangDests(): Promise<void> {
    const translationDestDir = path.join(translationDir, 'dest');
    const categoryDirs = await readdir(translationDestDir);
    for (const lang of otherLangs) {
      const i18n = await parseXmls(
        categoryDirs.map(cd => {
          return { name: categoryName(cd), path: path.join(translationDestDir, cd, `${lang}.xml`) };
        }),
      );
      langsI18n[lang] = i18n;
    }
  }

  async function writeI18n() {
    const coreKeys = new Set<string>();
    for (const name of Object.keys(packagesKeyCache)) {
      if (!separatePackages.includes(name)) {
        for (const key of packagesKeyCache[name]) {
          coreKeys.add(key);
        }
      }
    }

    const outputKeys: Record<string, Set<string>> = {};
    for (const name of separatePackages) {
      if (packagesKeyCache[name] !== undefined) {
        const keys = new Set<string>();
        for (const key of packagesKeyCache[name]) {
          if (!coreKeys.has(key)) keys.add(key);
        }
        outputKeys[name] = keys;
      } else {
        console.warn(`Cannot build ${name}${usedFilter ? ': --filter flag was used' : ''}`);
      }
    }
    outputKeys.core = coreKeys;

    for (const lang of [...otherLangs, defaultLang]) {
      const i18n = lang === defaultLang ? sourceI18n : langsI18n[lang];
      if (!i18n) console.warn(`No i18n for ${lang}`);

      for (const [name, keys] of Object.entries(outputKeys)) {
        const obj: Record<string, string> = {};

        for (const key of keys) {
          const value = i18n[key] || sourceI18n[key];
          if (typeof value === 'string') obj[key] = value;
          else if (typeof value === 'object') {
            for (const [pKey, pValue] of Object.entries(value)) {
              obj[`${key}|${pKey}`] = pValue;
            }
          } else console.warn(`No valid value found for: ${key} (${lang})`);
        }

        const code =
          name === 'core'
            ? `window.lishogi.i18n=${JSON.stringify(obj)};window.lishogi.quantity=${quantity(lang)};${timeago(lang)}`
            : `Object.assign(window.lishogi.i18n||{}, ${JSON.stringify(obj)});`;

        if (isValidJs(code)) await writeFile(path.join(outdir, name, `${lang}.js`), code);
        else console.error('Invalid code!', name, lang);
      }
    }
  }
}
