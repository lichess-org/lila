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
export function throttlePromise<T extends (...args: any) => Promise<void>>(
  delay: number,
  callback: T
): (...args: Parameters<T>) => void {
  let lastComplete = 0;
  let inFlight = false;
  let lastPending: boolean | Timeout = false;

  return function (this: any, ...args: Parameters<T>): void {
    const self = this;
    const elapsed = performance.now() - lastComplete;

    function exec() {
      lastPending = false;
      inFlight = true;
      callback.apply(self, args).finally(() => {
        inFlight = false;
        lastComplete = performance.now();
        if (lastPending === true) lastPending = setTimeout(exec, delay);
      });
    }

    if (!inFlight && elapsed > delay) exec();
    else if (inFlight) lastPending = true;
    else if (!lastPending) lastPending = setTimeout(exec, delay - elapsed);
  };
}
