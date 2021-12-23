/***
 * Wraps an asynchronous function to ensure only one call at a time is in
 * flight. Any extra calls are dropped, except the last one, which waits for
 * the previous call to complete.
 */
export function throttlePromise<T extends (...args: any) => Promise<void>>(
  callback: T
): (...args: Parameters<T>) => void {
  let current: Promise<void> | undefined;
  let afterCurrent: (() => void) | undefined;

  return function (this: any, ...args: Parameters<T>): void {
    const self = this;

    const exec = () => {
      afterCurrent = undefined;
      current = callback.apply(self, args).finally(() => {
        current = undefined;
        if (afterCurrent) afterCurrent();
      });
    };

    if (current) afterCurrent = exec;
    else exec();
  };
}

export function sleep(delay: number): () => Promise<void> {
  return () => new Promise(resolve => setTimeout(resolve, delay));
}

/**
 * Ensures calls to the wrapped function are spaced by the given delay.
 * Any extra calls are dropped, except the last one, which waits for the delay.
 */
export default function throttle<T extends (...args: any) => void>(
  delay: number,
  callback: T
): (...args: Parameters<T>) => void {
  return throttlePromise(function (this: any, ...args: Parameters<T>) {
    callback.apply(this, args);
    return sleep(delay)();
  });
}
