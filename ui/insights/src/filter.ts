import { InsightFilter, InsightFilterWithoutCustom, speeds, variants } from './types';

export function defaultFilter(isBot: boolean): InsightFilter {
  return {
    since: (isBot ? 90 : 365).toString(),
    variant: 'standard',
    color: 'both',
    rated: 'both',
    speeds: speeds,
    computer: 'no',
    custom: {
      type: 'game',
      x: 'color',
      y: 'nbOfMovesAndDrops',
    },
  };
}

export function filterOptions<K extends keyof InsightFilterWithoutCustom>(key: K): InsightFilterWithoutCustom[K][] {
  return allOptions[key] as InsightFilterWithoutCustom[K][];
}
export const allOptions = {
  since: [7, 30, 90, 365].map(n => n.toString()),
  variant: variants,
  color: ['both', 'sente', 'gote'],
  rated: ['both', 'yes', 'no'],
  speeds: speeds,
  computer: ['both', 'yes', 'no'],
};
