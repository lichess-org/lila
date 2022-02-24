export default function stepwiseScroll(
  inner: (e: WheelEvent, scroll: boolean) => void,
  threshold = 100
): (e: WheelEvent) => void {
  /** Track distance scrolled across multiple wheel events, resetting after 500 ms.  */
  let lastScrollForward: boolean;
  let scrollTotal = 0;
  let lastScrollTimestamp = -9999;
  let lastScrollStable: undefined | number | false; // last scroll delta, or false if previously found to be variable
  return (e: WheelEvent) => {
    const absDelta = Math.abs(e.deltaY);
    if (!absDelta) return;

    if (lastScrollStable == undefined) {
      // first scroll
      lastScrollStable = absDelta;
      lastScrollTimestamp = e.timeStamp;
      return inner(e, true);
    } else {
      // scroll amount is stable, assume step wheel
      if (absDelta == lastScrollStable) return inner(e, true);
      else lastScrollStable = false;
    }

    const forward = e.deltaY > 0;
    if (lastScrollForward != forward || e.timeStamp - lastScrollTimestamp > 500) scrollTotal = 0;
    lastScrollForward = forward;
    scrollTotal += absDelta;
    if (scrollTotal >= threshold) {
      inner(e, true);
      scrollTotal -= threshold;
    } else inner(e, false);

    lastScrollTimestamp = e.timeStamp;
  };
}
