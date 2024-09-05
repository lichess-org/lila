const subs: Dictionary<Set<() => void>> = Object.create(null);
const onces: Dictionary<any> = Object.create(null);

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
  once(name: string): Promise<any> {
    if (!onces[name]) {
      onces[name] = {};
      onces[name].promise = new Promise<any>(resolve => onces[name].resolve = resolve);
    }
    return onces[name].promise;
  },
  complete(name: string, data: any): void {
    if (onces[name]) onces[name].resolve?.(data);
    else onces[name] = { promise: Promise.resolve(data) };
    onces[name].resolve = undefined;
  },
};

export default pubsub;
