interface OneTimeHandler {
  promise: Promise<void>;
  resolve?: () => void;
}

const allSubs: Map<string, Set<() => void>> = new Map();
const oneTimeEvents: Map<string, OneTimeHandler> = new Map();

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
    const found = oneTimeEvents.get(event);
    if (found) return found.promise;

    const handler = {} as OneTimeHandler;
    handler.promise = new Promise<void>(resolve => handler!.resolve = resolve);
    oneTimeEvents.set(event, handler);

    return handler.promise;
  },
  complete(event: string): void {
    const found = oneTimeEvents.get(event);
    if (found) {
      found.resolve?.();
      found.resolve = undefined;
    }
    else oneTimeEvents.set(event, { promise: Promise.resolve() });
  },
};

export default pubsub;
