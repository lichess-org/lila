const allSubs: Map<string, Set<() => void>> = new Map();
const oneTimeEvents: Map<string, any> = new Map();

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
    if (!oneTimeEvents.get(event)) {
      oneTimeEvents.set(event, {});
      oneTimeEvents.get(event).promise = new Promise<void>(resolve => 
        oneTimeEvents.get(event).resolve = resolve
      );
    }
    return oneTimeEvents.get(event).promise;
  },
  complete(event: string): void {
    if (oneTimeEvents.get(event)) oneTimeEvents.get(event).resolve?.();
    else oneTimeEvents.set(event, { promise: Promise.resolve() });
    oneTimeEvents.get(event).resolve = undefined;
  },
};

export default pubsub;
