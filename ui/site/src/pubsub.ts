const subs: Dictionary<Set<() => void>> = Object.create(null);
const oneTimeEvents: Dictionary<any> = Object.create(null);

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
