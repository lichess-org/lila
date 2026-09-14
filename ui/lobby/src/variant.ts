import { storage } from 'lib/storage';
import { confirm } from 'lib/view';

type ActualVariant = Exclude<VariantKey, 'standard' | 'fromPosition'>;

const variantConfirm = (variant: ActualVariant): string =>
  `${i18n.variant[variant]}\n\n${i18n.variant[`${variant}Title`]}`;

const storageKey = (key: ActualVariant) => `lobby.variant.${key}`;

export default async function (variant?: VariantKey) {
  if (!variant || variant === 'standard' || variant === 'fromPosition' || storage.get(storageKey(variant)))
    return true;
  const confirmed = await confirm(variantConfirm(variant));
  if (confirmed) storage.set(storageKey(variant), '1');
  return confirmed;
}
