import { memoize } from './common';

export function stepwiseScroll(inner: (e: WheelEvent, scroll: boolean) => void): (e: WheelEvent) => void {
  let scrollTotal = 0;
  return (e: WheelEvent) => {
    scrollTotal += e.deltaY * (e.deltaMode ? 40 : 1);
    if (Math.abs(scrollTotal) >= 4) {
      inner(e, true);
      scrollTotal = 0;
    } else {
      inner(e, false);
    }
  };
}

export const elementScrollBarWidth = memoize<number>(() => {
  const ruler = document.createElement('div');
  ruler.style.position = 'absolute';
  ruler.style.overflow = 'scroll';
  ruler.style.visibility = 'hidden';
  document.body.appendChild(ruler);
  const barWidth = ruler.offsetWidth - ruler.clientWidth;
  document.body.removeChild(ruler);
  return barWidth;
});
