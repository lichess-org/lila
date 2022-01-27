export default function debounce<T extends (...args: any) => void>(
  f: T,
  wait: number,
  immediate = false
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
