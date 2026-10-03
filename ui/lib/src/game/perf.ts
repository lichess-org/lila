// Ordered by scalachess variant ids
export const variants: VariantKey[] = [
  'standard',
  'chess960',
  'fromPosition',
  'kingOfTheHill',
  'threeCheck',
  'antichess',
  'atomic',
  'horde',
  'racingKings',
  'crazyhouse',
];

export const perfIsVariant = (perf: Speed | VariantKey): perf is VariantKey =>
  variants.includes(perf as VariantKey);

// !! Must have i18n.variant loaded !!
export const perfName = (perf: Speed | VariantKey): string =>
  perfIsVariant(perf) ? i18n.variant[perf] : i18n.site[perf];
