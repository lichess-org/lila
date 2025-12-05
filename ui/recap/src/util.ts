import type { RecapPerf } from './interfaces';

export function formatDuration(seconds: number, glue = '<br>'): string {
  const d = Math.floor(seconds / (24 * 3600));
  const h = Math.floor((seconds % (24 * 3600)) / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  const result: string[] = [];
  if (d > 0) {
    result.push(i18n.site.nbDays(d));
  }
  result.push(i18n.site.nbHours(h));
  result.push(i18n.site.nbMinutes(m));

  return result.slice(0, 2).join(glue);
}

export const perfIsSpeed = (p: Perf): p is Speed =>
  (['ultraBullet', 'bullet', 'blitz', 'rapid', 'classical', 'correspondence'] as const).includes(p as Speed);

export const perfLabel = (p: RecapPerf): string =>
  perfIsSpeed(p.key) ? i18n.recap.shareableFavouriteTimeControl : i18n.recap.shareableFavouriteVariant;
