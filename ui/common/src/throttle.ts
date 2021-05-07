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
