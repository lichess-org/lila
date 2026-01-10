import { h } from 'snabbdom';
import type { MaybeVNodes } from 'lib/view';

export function tds(bits: MaybeVNodes): MaybeVNodes {
  return bits.map(bit => h('td', [bit]));
}

export const perfNames = {
  ultraBullet: i18n.site.ultraBullet,
  bullet: i18n.site.bullet,
  blitz: i18n.site.blitz,
  rapid: i18n.site.rapid,
  classical: i18n.site.classical,
  correspondence: i18n.site.correspondence,
  racingKings: i18n.variant.racingKings,
  threeCheck: i18n.variant.threeCheck,
  antichess: i18n.variant.antichess,
  horde: i18n.variant.horde,
  atomic: i18n.variant.atomic,
  crazyhouse: i18n.variant.crazyhouse,
  chess960: i18n.variant.chess960,
  kingOfTheHill: i18n.variant.kingOfTheHill,
};
