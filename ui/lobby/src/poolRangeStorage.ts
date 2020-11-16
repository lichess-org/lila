function makeKey(poolId) {
  return "lobby-pool-range-" + poolId;
}

export function set(poolId, range) {
  const key = makeKey(poolId);
  if (range) window.lishogi.storage.set(key, range);
  else window.lishogi.storage.remove(key);
}

export function get(poolId) {
  return window.lishogi.storage.get(makeKey(poolId));
}
