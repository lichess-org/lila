export const speeds: Record<Speed, string> = {
  ultraBullet: i18n.site.ultraBullet,
  bullet: i18n.site.bullet,
  blitz: i18n.site.blitz,
  rapid: i18n.site.rapid,
  classical: i18n.site.classical,
  correspondence: i18n.site.correspondence,
};

export const variants: Record<VariantKey, string> = {
  standard: i18n.variant.standard,
  chess960: i18n.variant.chess960,
  kingOfTheHill: i18n.variant.kingOfTheHill,
  threeCheck: i18n.variant.threeCheck,
  antichess: i18n.variant.antichess,
  atomic: i18n.variant.atomic,
  horde: i18n.variant.horde,
  racingKings: i18n.variant.racingKings,
  crazyhouse: i18n.variant.crazyhouse,
  fromPosition: i18n.variant.fromPosition,
};

export const perfNames: Record<Perf | 'standard', string> = {
  ...speeds,
  ...variants,
};
