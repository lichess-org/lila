import { isMac } from '@/device';

export default function stepwiseScroll(
  scrollAction: (e: WheelEvent) => void,
  shouldSkip: (e: WheelEvent) => boolean,
  ifSkipShouldStillPreventDefault?: boolean,
): (e: WheelEvent) => void {
  let accumulatedDeltaPixelMode = 0;
  return (e: WheelEvent) => {
    if (e.ctrlKey) return; // if touchpad zooming, e.ctrlKey is true
    if (shouldSkip(e)) {
      if (ifSkipShouldStillPreventDefault) e.preventDefault();
      return;
    }
    e.preventDefault();
    if (e.deltaMode === 0) {
      accumulatedDeltaPixelMode += e.deltaY;
      if (isMac() && Math.abs(accumulatedDeltaPixelMode) < 10) return;
    }
    accumulatedDeltaPixelMode = 0;
    scrollAction(e);
  };
}
