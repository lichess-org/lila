/***
 * Wraps an asynchronous function to ensure only one call at a time is in
 * flight. Any extra calls are dropped, except the last one, which waits for
 * the previous call to complete.
 */
// no side effects allowed due to re-export by index.ts

export function throttlePromiseWithResult<R, T extends (...args: any) => Promise<R>>(
  wrapped: T,
): (...args: Parameters<T>) => Promise<R> {
  let current: Promise<R> | undefined;
  let pending:
    | {
        run: () => Promise<R>;
        reject: () => void;
      }
    | undefined;

  return function (this: any, ...args: Parameters<T>): Promise<R> {
    const self = this;

    const runCurrent = () => {
      current = wrapped.apply(self, args).finally(() => {
        current = undefined;
        if (pending) {
          pending.run();
          pending = undefined;
        }
      });
      return current;
    };

    if (!current) return runCurrent();

    pending?.reject();
    const next = new Promise<R>((resolve, reject) => {
      pending = {
        run: () =>
          runCurrent().then(
            res => {
              resolve(res);
              return res;
            },
            err => {
              reject(err);
              throw err;
            },
          ),
        reject: () => reject(new Error('Throttled')),
      };
    });
    return next;
  };
}

/* doesn't fail the promise if it's throttled */
export function throttlePromise<T extends (...args: any) => Promise<void>>(
  wrapped: T,
): (...args: Parameters<T>) => Promise<void> {
  const throttler = throttlePromiseWithResult<void, T>(wrapped);
  return function (this: any, ...args: Parameters<T>): Promise<void> {
    return throttler.apply(this, args).catch(() => {});
  };
}

/**
 * Wraps an asynchronous function to return a promise that resolves
 * after completion plus a delay (regardless if the wrapped function resolves
 * or rejects).
 */
export function finallyDelay<T extends (...args: any) => Promise<any>>(
  delay: (...args: Parameters<T>) => number,
  wrapped: T,
): (...args: Parameters<T>) => Promise<void> {
  return function (this: any, ...args: Parameters<T>): Promise<void> {
    const self = this;
    return new Promise(resolve => {
      wrapped.apply(self, args).finally(() => setTimeout(resolve, delay.apply(self, args)));
    });
  };
}

/**
 * Wraps an asynchronous function to ensure only one call at a time is in flight. Any extra calls
 * are dropped, except the last one, which waits for the previous call to complete plus a delay.
 */
export function throttlePromiseDelay<T extends (...args: any) => Promise<any>>(
  delay: (...args: Parameters<T>) => number,
  wrapped: T,
): (...args: Parameters<T>) => Promise<void> {
  return throttlePromise(finallyDelay(delay, wrapped));
}

/**
 * Ensures calls to the wrapped function are spaced by the given delay.
 * Any extra calls are dropped, except the last one, which waits for the delay.
 */
export function throttle<T extends (...args: any) => void>(
  delay: number,
  wrapped: T,
): (...args: Parameters<T>) => void {
  return throttlePromise(function (this: any, ...args: Parameters<T>) {
    wrapped.apply(this, args);
    return new Promise(resolve => setTimeout(resolve, delay));
  });
}

export interface Sync<T> {
  promise: Promise<T>;
  sync: T | undefined;
}

export function sync<T>(promise: Promise<T>): Sync<T> {
  const sync: Sync<T> = {
    sync: undefined,
    promise: promise.then(v => {
      sync.sync = v;
      return v;
    }),
  };
  return sync;
}

// Call an async function with a maximum time limit (in milliseconds) for the timeout
export async function promiseTimeout<A>(asyncPromise: Promise<A>, timeLimit: number): Promise<A> {
  let timeoutHandle: Timeout | undefined = undefined;

  const timeoutPromise = new Promise<A>((_, reject) => {
    timeoutHandle = setTimeout(() => reject(new Error('Async call timeout limit reached')), timeLimit);
  });

  const result = await Promise.race([asyncPromise, timeoutPromise]);
  if (timeoutHandle) clearTimeout(timeoutHandle);
  return result;
}

export function debounce<T extends (...args: any) => void>(
  f: T,
  wait: number,
  immediate = false,
): (...args: Parameters<T>) => void {
  let timeout: Timeout | undefined;
  let lastBounce = 0;

  return function (this: any, ...args: Parameters<T>) {
    const self = this;

    if (timeout) clearTimeout(timeout);
    timeout = undefined;

    const elapsed = performance.now() - lastBounce;
    lastBounce = performance.now();
    if (immediate && elapsed > wait) f.apply(self, args);
    else
      timeout = setTimeout(() => {
        timeout = undefined;
        f.apply(self, args);
      }, wait);
  };
}

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
