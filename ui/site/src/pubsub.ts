const allSubs: Map<string, Set<() => void>> = new Map();

const pubsub: Pubsub = {
  on(name: string, cb) {
    const subs = allSubs.get(name);
    if (subs) subs.add(cb);
    else allSubs.set(name, new Set([cb]));
  },
  off(name: string, cb) {
    allSubs.get(name)?.delete(cb);
  },
  emit(name: string, ...args: any[]) {
    for (const fn of allSubs.get(name) || []) fn.apply(null, args);
  },
};

export default pubsub;
