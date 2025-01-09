import { i18n } from './i18n';
import { i18nVariant } from './variant';

export function i18nPerf(key: Perf): string;
export function i18nPerf(key: string): string | undefined;
export function i18nPerf(str: Perf): string | undefined {
  switch (str) {
    case 'ultraBullet':
      return i18n('ultrabullet');
    case 'bullet':
      return i18n('bullet');
    case 'blitz':
      return i18n('blitz');
    case 'rapid':
      return i18n('rapid');
    case 'classical':
      return i18n('classical');
    case 'correspondence':
      return i18n('correspondence');
    default:
      return i18nVariant(str);
  }
}
