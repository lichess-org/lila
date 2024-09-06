const subs: Dictionary<Set<() => void>> = Object.create(null);
const oneTimeEvents: Dictionary<any> = Object.create(null);

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
  after(event: string): Promise<void> {
    if (!oneTimeEvents[event]) {
      oneTimeEvents[event] = {};
      oneTimeEvents[event].promise = new Promise<void>(resolve => oneTimeEvents[event].resolve = resolve);
    }
    return oneTimeEvents[event].promise;
  },
  complete(event: string): void {
    if (oneTimeEvents[event]) oneTimeEvents[event].resolve?.();
    else oneTimeEvents[event] = { promise: Promise.resolve() };
    oneTimeEvents[event].resolve = undefined;
  },
};

export default pubsub;
