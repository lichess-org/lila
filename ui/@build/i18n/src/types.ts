import { pluralCategories } from './constants.js';

export type PluralCategory = (typeof pluralCategories)[number];

export type I18nObj = Record<string, string | Partial<Record<PluralCategory, string>>>;

export interface XmlSource {
  name: string;
  path: string;
}
