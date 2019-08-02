const variantConfirms = {
  'frisian': 'This is a Frisian Draughts game!\n\nPieces can also capture horizontally and vertically.',
  'frysk': 'This is a Frysk! game!\n\nFrisian draughts starting with 5 pieces each.',
  'antidraughts': 'This is an Antidraughts game!\n\nThe game can be won by losing all your pieces, or running out of moves.',
  'breakthrough': 'This is a Breakthrough game!\n\nThe first player who makes a king wins.'
};

function storageKey(key) {
  return 'lobby.variant.' + key;
}

export default function(variant: string) {
  return Object.keys(variantConfirms).every(function(key) {
    const v = variantConfirms[key]
      if (variant === key && !window.lidraughts.storage.get(storageKey(key))) {
        const c = confirm(v);
        if (c) window.lidraughts.storage.set(storageKey(key), '1');
        return c;
      } else return true;
  })
}
