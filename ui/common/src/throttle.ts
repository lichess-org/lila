/***
 * Wraps an asynchronous function to ensure only one call at a time is in
 * flight. Any extra calls are dropped, except the last one, which waits for
 * the previous call to complete.
 */
export function throttlePromise<T extends (...args: any) => Promise<any>>(
  wrapped: T,
): (...args: Parameters<T>) => void {
  let current: Promise<void> | undefined;
  let afterCurrent: (() => void) | undefined;

  return function (this: any, ...args: Parameters<T>): void {
    const self = this;

    const exec = () => {
      afterCurrent = undefined;
      current = wrapped.apply(self, args).finally(() => {
        current = undefined;
        if (afterCurrent) afterCurrent();
      });
    };

    if (current) afterCurrent = exec;
    else exec();
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
): (...args: Parameters<T>) => void {
  return throttlePromise(finallyDelay(delay, wrapped));
}

/**
 * Ensures calls to the wrapped function are spaced by the given delay.
 * Any extra calls are dropped, except the last one, which waits for the delay.
 */
export default function throttle<T extends (...args: any) => void>(
  delay: number,
  wrapped: T,
): (...args: Parameters<T>) => void {
  return throttlePromise(function (this: any, ...args: Parameters<T>) {
    wrapped.apply(this, args);
    return new Promise(resolve => setTimeout(resolve, delay));
  });
}
