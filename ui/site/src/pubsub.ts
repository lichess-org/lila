const subs: Dictionary<Set<() => void>> = Object.create(null);

const pubsub: Pubsub = {
  on(name: string, cb) {
    (subs[name] = subs[name] || new Set()).add(cb);
  },
  off(name: string, cb) {
    subs[name]?.delete(cb);
  },
  emit(name: string, ...args: any[]) {
    for (const fn of subs[name] || []) fn.apply(null, args);
  },
};

export default pubsub;
