import { isMac } from '@/device';

export default function stepwiseScroll(
  scrollAction: (e: WheelEvent) => void,
  shouldSkip: (e: WheelEvent) => boolean,
  ifSkipShouldStillPreventDefault?: boolean,
): (e: WheelEvent) => void {
  let accumulatedDelta = 0;
  return (e: WheelEvent) => {
    if (e.ctrlKey) return; // if touchpad zooming, e.ctrlKey is true
    if (shouldSkip(e)) {
      if (ifSkipShouldStillPreventDefault) e.preventDefault();
      return;
    }
    e.preventDefault();
    if (accumulatedDelta * e.deltaY < 0) accumulatedDelta = 0;
    accumulatedDelta += e.deltaY;
    if (Math.abs(accumulatedDelta) < wheelClickDelta(e)) return;
    accumulatedDelta = 0;
    scrollAction(e);
  };
}

// what one click of a classic wheel adds up to, so that a high resolution wheel splitting a click
// into several small events still steps one move. Macs accelerate wheel deltas and keep a small threshold.
const wheelClickDelta = (e: WheelEvent): number => {
  if (e.deltaMode === 0) return isMac() ? 10 : 100;
  if (e.deltaMode === 1) return isMac() ? 1 : 3;
  return 1;
};
