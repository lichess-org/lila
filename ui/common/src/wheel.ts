export default function shouldScroll(e: WheelEvent, threshold: number): boolean {
  /** Track distance scrolled across multiple wheel events, resetting after 500 ms.  */
  const lastScrollDirection = lichess.tempStorage.get('lastScrollDirection');
  let scrollTotal = parseInt(lichess.tempStorage.get('scrollTotal') || '0');
  const lastScrollTimestamp = parseFloat(lichess.tempStorage.get('lastScrollTimestamp') || '-9999');
  let ret = false;
  if (e.deltaY > 0) {
    if (lastScrollDirection != 'forward' || e.timeStamp - lastScrollTimestamp > 500) scrollTotal = 0;
    lichess.tempStorage.set('lastScrollDirection', 'forward');
    scrollTotal += e.deltaY;
    if (scrollTotal >= threshold) {
      ret = true;
      scrollTotal -= threshold;
    }
  } else if (e.deltaY < 0) {
    if (lastScrollDirection != 'back' || e.timeStamp - lastScrollTimestamp > 500) scrollTotal = 0;
    lichess.tempStorage.set('lastScrollDirection', 'back');
    scrollTotal -= e.deltaY;
    if (scrollTotal >= threshold) {
      ret = true;
      scrollTotal -= threshold;
    }
  }
  lichess.tempStorage.set('scrollTotal', scrollTotal.toString());
  lichess.tempStorage.set('lastScrollTimestamp', e.timeStamp.toString());
  return ret;
}
