
function storageKey(key) {
  return 'lobby.variant.' + key;
}

export default function (variant: VariantKey, trans: TransNoArg) {
  if (!window.lishogi.storage.get(storageKey(variant))) {
    const v = trans(variant),
      c = confirm(v);
    if (c) window.lishogi.storage.set(storageKey(variant), '1');
    return c;
  } else return true;
}
