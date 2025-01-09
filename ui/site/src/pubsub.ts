const allSubs: Map<string, Set<PubsubCallback>> = new Map();

export const pubsub: Pubsub = {
  on(name: string, cb: PubsubCallback) {
    const subs = allSubs.get(name);
    if (subs) subs.add(cb);
    else allSubs.set(name, new Set([cb]));
  },
  off(name: string, cb: PubsubCallback) {
    allSubs.get(name)?.delete(cb);
  },
  emit(name, ...args) {
    for (const fn of allSubs.get(name) || []) fn.apply(null, args);
  },
};
