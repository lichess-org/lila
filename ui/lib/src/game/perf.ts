export const variants: VariantKey[] = [
  'standard',
  'chess960',
  'kingOfTheHill',
  'threeCheck',
  'antichess',
  'atomic',
  'horde',
  'racingKings',
  'crazyhouse',
  'fromPosition',
];

export const perfIsVariant = (perf: Speed | VariantKey): perf is VariantKey =>
  variants.includes(perf as VariantKey);

export const perfName = (perf: Speed | VariantKey): string =>
  perfIsVariant(perf) ? i18n.variant[perf] : i18n.site[perf];
