const subs: Dictionary<(() => void)[]> = Object.create(null);

const pubsub: Pubsub = {
  on(name: string, cb) {
    (subs[name] = subs[name] || []).push(cb);
  },
  off(name: string, cb) {
    const cbs = subs[name];
    if (cbs)
      for (const i in cbs) {
        if (cbs[i] === cb) {
          cbs.splice(+i);
          break;
        }
      }
  },
  emit(name: string, ...args: any[]) {
    for (const fn of subs[name] || []) fn.apply(null, args);
  },
};

export default pubsub;
