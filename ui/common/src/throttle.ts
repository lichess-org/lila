// Ensures calls to the wrapped function are spaced by the given delay.
// Any extra calls are dropped, except the last one.
export default function throttle(delay: number, callback: (...args: any[]) => void): (...args: any[]) => void {
  let timer: number | undefined;
  let lastExec = 0;

  return function (this: any, ...args: any[]): void {
    const self: any = this;
    const elapsed = performance.now() - lastExec;

    function exec() {
      timer = undefined;
      lastExec = performance.now();
      callback.apply(self, args);
    }

    if (timer) clearTimeout(timer);

    if (elapsed > delay) exec();
    else timer = setTimeout(exec, delay - elapsed);
  };
}
