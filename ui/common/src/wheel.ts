export default function stepwiseScroll(
  inner: (e: WheelEvent, scroll: boolean) => void,
  threshold: number = 120
): (e: WheelEvent) => void {
  /** Track distance scrolled across multiple wheel events, resetting after 500 ms.  */
  let lastScrollDirection: string;
  let scrollTotal = 0;
  let lastScrollTimestamp = -9999;
  return (e: WheelEvent) => {
    if (e.deltaY > 0) {
      if (lastScrollDirection != 'forward' || e.timeStamp - lastScrollTimestamp > 500) scrollTotal = 0;
      lastScrollDirection = 'forward';
      scrollTotal += e.deltaY;
      if (scrollTotal >= threshold) {
        inner(e, true);
        scrollTotal -= threshold;
      } else inner(e, false);
    } else if (e.deltaY < 0) {
      if (lastScrollDirection != 'back' || e.timeStamp - lastScrollTimestamp > 500) scrollTotal = 0;
      lastScrollDirection = 'back';
      scrollTotal -= e.deltaY;
      if (scrollTotal >= threshold) {
        inner(e, true);
        scrollTotal -= threshold;
      } else inner(e, false);
    }
    lastScrollTimestamp = e.timeStamp;
  };
}
