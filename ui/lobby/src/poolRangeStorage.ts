import { LobbyMe, PoolMember } from './interfaces';

const makeKey = (me: LobbyMe | undefined, poolId: string) =>
  `lobby-pool-range.${me?.username || 'anon'}.${poolId}`;

export const set = (me: LobbyMe | undefined, member: PoolMember) => {
  const key = makeKey(me, member.id);
  if (member.range) site.storage.set(key, member.range);
  else site.storage.remove(key);
};

export const get = (me: LobbyMe | undefined, poolId: string) => site.storage.get(makeKey(me, poolId));
