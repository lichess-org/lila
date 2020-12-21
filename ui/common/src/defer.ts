export function defer<A>(): DeferPromise.Deferred<A> {
  const deferred: Partial<DeferPromise.Deferred<A>> = {};
  deferred.promise = new Promise<A>((resolve, reject) => {
    deferred.resolve = resolve;
    deferred.reject = reject;
  });
  return deferred as DeferPromise.Deferred<A>;
}
