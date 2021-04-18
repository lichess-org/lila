const variantConfirms = {
};

function storageKey(key) {
  return "lobby.variant." + key;
}

export default function (variant: string) {
  return Object.keys(variantConfirms).every(function (key) {
    const v = variantConfirms[key];
    if (variant === key && !window.lishogi.storage.get(storageKey(key))) {
      const c = confirm(v);
      if (c) window.lishogi.storage.set(storageKey(key), "1");
      return c;
    } else return true;
  });
}
