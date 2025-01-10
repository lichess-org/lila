import { readFile } from 'node:fs/promises';
import { XMLParser } from 'fast-xml-parser';
import type { I18nObj, PluralCategory, XmlSource } from './types.js';

interface StringResource {
  '@_name': string;
  '#text': string;
}

interface PluralItem {
  '@_quantity': PluralCategory;
  '#text': string;
}

interface PluralsResource {
  '@_name': string;
  item: PluralItem[];
}

interface Resources {
  string?: StringResource[];
  plurals?: PluralsResource[];
}

interface ParsedResources {
  resources: Resources;
}

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '@_',
  textNodeName: '#text',
});

export async function parseXmls(sources: XmlSource[]): Promise<I18nObj> {
  const keysObjects = await Promise.all(sources.map(s => parseXml(s)));
  return keysObjects.reduce((acc, obj) => ({ ...acc, ...obj }), {});
}

async function parseXml(source: XmlSource): Promise<I18nObj> {
  const result: I18nObj = {};
  try {
    const xmlContent = await readFile(source.path, 'utf-8'),
      prefix = source.name === 'site' ? '' : `${source.name}:`,
      parsed = parser.parse(xmlContent) as ParsedResources;

    if (parsed.resources.string) {
      const strings = Array.isArray(parsed.resources.string)
        ? parsed.resources.string
        : [parsed.resources.string];

      strings.forEach(str => {
        const key = `${prefix}${str['@_name']}`;
        result[key] = str['#text'];
      });
    }

    if (parsed.resources.plurals) {
      const plurals = Array.isArray(parsed.resources.plurals)
        ? parsed.resources.plurals
        : [parsed.resources.plurals];

      plurals.forEach(plural => {
        const baseKey = `${prefix}${plural['@_name']}`,
          items = Array.isArray(plural.item) ? plural.item : [plural.item];

        result[baseKey] = items.reduce(
          (acc, item) => {
            acc[item['@_quantity']] = item['#text'];
            return acc;
          },
          {} as Partial<Record<PluralCategory, string>>,
        );
      });
    }

    return result;
  } catch (error) {
    console.error(`Error processing file ${source.path}:`, error);
    return result;
  }
}
