function makeKey(poolId: string) {
  return 'lobby-pool-range-' + poolId;
}

export function set(poolId: string, range?: string) {
  const key = makeKey(poolId);
  if (range) lichess.storage.set(key, range);
  else lichess.storage.remove(key);
}

export function get(poolId: string) {
  return lichess.storage.get(makeKey(poolId));
}
