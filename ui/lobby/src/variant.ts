import { variants as variantNames } from 'lib/game/perf';
import { storage } from 'lib/storage';
import { confirm } from 'lib/view';

import { variants } from './options';

type ActualVariant = Exclude<VariantKey, 'standard' | 'fromPosition'>;

const variantConfirm = (variant: ActualVariant): string => {
  const v = variants.find(({ key }) => key === variant)!;
  return `${variantNames[v.key]}\n\n${v.description}`;
};

const storageKey = (key: ActualVariant) => `lobby.variant.${key}`;

export default async function (variant?: VariantKey) {
  if (!variant || variant === 'standard' || variant === 'fromPosition' || storage.get(storageKey(variant)))
    return true;
  const confirmed = await confirm(variantConfirm(variant));
  if (confirmed) storage.set(storageKey(variant), '1');
  return confirmed;
}
