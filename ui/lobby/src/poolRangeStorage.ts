import { LobbyMe, PoolMember } from './interfaces';
import { storage } from 'common/storage';

const makeKey = (me: LobbyMe | undefined, poolId: string) =>
  `lobby-pool-range.${me?.username || 'anon'}.${poolId}`;

export const set = (me: LobbyMe | undefined, member: PoolMember) => {
  const key = makeKey(me, member.id);
  if (member.range) storage.set(key, member.range);
  else storage.remove(key);
};

export const get = (me: LobbyMe | undefined, poolId: string) => storage.get(makeKey(me, poolId));
