function makeKey(poolId) {
  return 'lobby-pool-range-' + poolId;
}

export function set(poolId, range) {
  const key = makeKey(poolId);
  if (range) window.lichess.storage.set(key, range);
  else window.lichess.storage.remove(key);
}

export function get(poolId) {
  return window.lichess.storage.get(makeKey(poolId));
}
