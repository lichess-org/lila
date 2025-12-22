import { storage } from 'lib/storage';
import { confirm } from 'lib/view';

const variantConfirms: Record<string, string> = {
  chess960: `${i18n.variant.chess960}\n\n${i18n.variant.chess960Title}`,
  kingOfTheHill: `${i18n.variant.kingOfTheHill}\n\n${i18n.variant.kingOfTheHillTitle}`,
  threeCheck: `${i18n.variant.threeCheck}\n\n${i18n.variant.threeCheckTitle}`,
  antichess: `${i18n.variant.antichess}\n\n${i18n.variant.antichessTitle}`,
  atomic: `${i18n.variant.atomic}\n\n${i18n.variant.atomicTitle}`,
  horde: `${i18n.variant.horde}\n\n${i18n.variant.hordeTitle}`,
  racingKings: `${i18n.variant.racingKings}\n\n${i18n.variant.racingKingsTitle}`,
  crazyhouse: `${i18n.variant.crazyhouse}\n\n${i18n.variant.crazyhouseTitle}`,
};

const storageKey = (key: string) => 'lobby.variant.' + key;

export default async function (variant: string | undefined) {
  if (!variant || !variantConfirms[variant] || storage.get(storageKey(variant))) return true;
  const confirmed = await confirm(variantConfirms[variant]);
  if (confirmed) storage.set(storageKey(variant), '1');
  return confirmed;
}
