export interface Deferred<A> {
  promise: Promise<A>;
  resolve(a: A | PromiseLike<A>): void;
  reject(err: unknown): void;
}

export function defer<A>(): Deferred<A> {
  const deferred: Partial<Deferred<A>> = {};
  deferred.promise = new Promise<A>((resolve, reject) => {
    deferred.resolve = resolve;
    deferred.reject = reject;
  });
  return deferred as Deferred<A>;
}
