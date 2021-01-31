function makeKey(poolId) {
  return 'lobby-pool-range-' + poolId;
}

export function set(poolId, range) {
  const key = makeKey(poolId);
  if (range) lichess.storage.set(key, range);
  else lichess.storage.remove(key);
}

export function get(poolId) {
  return lichess.storage.get(makeKey(poolId));
}
