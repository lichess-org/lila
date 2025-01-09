import { i18n } from 'i18n';
import { i18nVariant } from 'i18n/variant';

function storageKey(key) {
  return 'lobby.variant.' + key;
}

export default function (variant: VariantKey): boolean {
  if (variant !== 'standard' && !window.lishogi.storage.get(storageKey(variant))) {
    const txt = `${i18n('variant')} - ${i18nVariant(variant)}`,
      c = confirm(txt);
    if (c) window.lishogi.storage.set(storageKey(variant), '1');
    return c;
  } else return true;
}
