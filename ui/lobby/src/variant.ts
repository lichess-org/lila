function storageKey(key) {
  return 'lobby.variant.' + key;
}

export default function (variant: VariantKey, trans: TransNoArg) {
  if (variant !== 'standard' && !window.lishogi.storage.get(storageKey(variant))) {
    const txt = `${trans('variant')} - ${trans(variant)}`,
      c = confirm(txt);
    if (c) window.lishogi.storage.set(storageKey(variant), '1');
    return c;
  } else return true;
}
