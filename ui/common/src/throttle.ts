// Ensures calls to the wrapped function are spaced by the given delay.
// Any extra calls are dropped, except the last one.
export default function throttle<T extends (...args: any) => void>(
  delay: number,
  callback: T
): (...args: Parameters<T>) => void {
  let timeout: Timeout | undefined;
  let lastExec = 0;

  return function (this: any, ...args: Parameters<T>): void {
    const self = this;
    const elapsed = performance.now() - lastExec;

    function exec() {
      timeout = undefined;
      lastExec = performance.now();
      callback.apply(self, args);
    }

    if (timeout) clearTimeout(timeout);

    if (elapsed > delay) exec();
    else timeout = setTimeout(exec, delay - elapsed);
  };
}

// Ensures calls to the wrapped function are spaced by the given delay,
// plus the duration of the promise.
// Any extra calls are dropped, except the last one.
export function throttlePromise<T extends (...args: any) => Promise<any>>(
  delay: number,
  callback: T
): (...args: Parameters<T>) => void {
  let timeout: Timeout | undefined;
  let lastComplete = 0;
  let onFlight: Promise<any> | undefined;

  return function (this: any, ...args: Parameters<T>): void {
    const self = this;
    const elapsed = performance.now() - lastComplete;

    function exec() {
      timeout = undefined;
      onFlight = callback.apply(self, args).finally(() => {
        onFlight = undefined;
        lastComplete = performance.now();
      });
    }

    if (timeout) clearTimeout(timeout);

    if (elapsed > delay && !onFlight) exec();
    else
      timeout = setTimeout(() => {
        if (onFlight) onFlight.finally(exec);
        else exec();
      }, delay - elapsed);
  };
}
