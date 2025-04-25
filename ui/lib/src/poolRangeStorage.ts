import { storage } from './storage';

const makeKey = (username: string | undefined, poolId: string) =>
  `lobby-pool-range.${username || 'anon'}.${poolId}`;

export const set = (username: string | undefined, poolId: string, range: string | undefined): void => {
  const key = makeKey(username, poolId);
  if (range) storage.set(key, range);
  else storage.remove(key);
};

export const shiftRange = (username: string | undefined, poolId: string, delta: number): void => {
  const currRange = get(username, poolId);
  if (!currRange) return;
  const [min, max] = currRange.split('-').map(Number);
  set(username, poolId, `${min + delta}-${max + delta}`);
};

export const get = (username: string | undefined, poolId: string): string | null =>
  storage.get(makeKey(username, poolId));
