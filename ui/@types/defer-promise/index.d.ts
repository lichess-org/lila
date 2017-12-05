interface Deferred<T> {
  promise: Promise<T>;
  resolve(value: T | PromiseLike<T>): void;
  resolve(this: Deferred<void>): void;
  reject(reason?: any): void;
}

declare module 'defer-promise' {
  function defer<T>(): Deferred<T>;
  export = defer;
}
