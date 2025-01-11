import { capitalize } from 'common/string';
import type { I18nKey } from './i18n-keys';

const i18nRecord: Record<string, string> = (window.lishogi as any).i18n || {};
const quantity: (c: number) => 'zero' | 'one' | 'two' | 'few' | 'many' | 'other' =
  (window.lishogi as any).quantity || (() => 'other');

export const i18n = (key: I18nKey): string => {
  return i18nRecord[key] || key;
};

const interpolate = (str: string, args: any[]): string => {
  if (args.length && str.includes('$s')) {
    for (let i = 1; i < 4; i++) {
      str = str.replace(`%${i}$s`, args[i - 1]);
    }
  }
  args.forEach(arg => {
    str = str.replace('%s', arg);
  });
  return str;
};

export const i18nFormat = (key: I18nKey, ...args: any[]): string => {
  const str = i18nRecord[key];
  return str ? interpolate(str, args) : `${key} ${args.join(', ')}`;
};

// args can be at the start
export const i18nFormatCapitalized = (key: I18nKey, ...args: any[]): string => {
  const str = i18nRecord[key];
  return capitalize(str ? interpolate(str, args) : `${key} ${args.join(', ')}`);
};

export const i18nPlural = (key: I18nKey, count: number, ...args: any[]): string => {
  const pluralKey = `${key}|${quantity(count)}`;
  const str = i18nRecord[pluralKey] || i18nRecord[key];
  return str ? interpolate(str, args) : `${key} ${count} ${args.join(', ')}`;
};

// count is both quantity and arg
export const i18nPluralSame = (key: I18nKey, count: number): string => {
  return i18nPlural(key, count, count);
};

const vdomInterpolate = (str: string, args: any[]): string[] => {
  const segments = str.split(/(%(?:\d\$)?s)/g);
  for (let i = 1; i <= args.length; i++) {
    const pos = segments.indexOf(`%${i}$s`);
    if (pos !== -1) segments[pos] = args[i - 1];
  }
  for (let i = 0; i < args.length; i++) {
    const pos = segments.indexOf('%s');
    if (pos === -1) break;
    segments[pos] = args[i];
  }
  return segments;
};

export const i18nVdom = (key: I18nKey, ...args: any[]): string[] => {
  const str = i18nRecord[key];
  return str ? vdomInterpolate(str, args) : [`${key} ${args.join(', ')}`];
};

export const i18nVdomPlural = (key: I18nKey, count: number, ...args: any[]): string[] => {
  const pluralKey = `${key}|${quantity(count)}`;
  const str = i18nRecord[pluralKey] || i18nRecord[key];
  return str ? vdomInterpolate(str, args) : [`${key} ${count} ${args.join(', ')}`];
};
