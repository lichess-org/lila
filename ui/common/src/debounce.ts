type Debounced = (...args: any[]) => any;

export default function debounce(func: (...args: any[]) => any, wait: number, immediate = false): Debounced {
  let timeout: Timeout | undefined,
    lastBounce = 0;
  return function (this: any) {
    let context = this,
      args = arguments,
      elapsed = performance.now() - lastBounce;
    lastBounce = performance.now();
    let later = () => {
      timeout = undefined;
      func.apply(context, args);
    };
    clearTimeout(timeout);
    if (immediate && elapsed > wait) func.apply(context, args);
    else timeout = setTimeout(later, wait);
  };
}
