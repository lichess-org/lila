function makeKey(poolId) {
  return 'lobby-pool-range-' + poolId;
}

export function set(poolId, range) {
  const key = makeKey(poolId);
  if (range) window.lidraughts.storage.set(key, range);
  else window.lidraughts.storage.remove(key);
}

export function get(poolId) {
  return window.lidraughts.storage.get(makeKey(poolId));
}
