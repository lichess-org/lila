const supported: boolean = 'wakeLock' in navigator;
let shouldLock: boolean = false;
let sentinel: WakeLockSentinel;

export const request = () => {
  if (supported) {
    navigator.wakeLock
      .request('screen')
      .then((response: WakeLockSentinel) => {
        shouldLock = true;
        sentinel = response;
      })
      .catch((error: Error) => {
        console.error('wakeLock - request failure. ' + error.message);
      });
  }
};

export const release = () => {
  if (supported) {
    sentinel
      .release()
      .then(() => {
        shouldLock = false;
      })
      .catch((error: Error) => {
        console.error('wakeLock - release failure. ' + error.message);
      });
  }
};

/* Since the act of switching tabs automatically releases
 * the wake lock, we re-request wake lock here based on the
 * `shouldLock` flag.
 */
document.addEventListener('visibilitychange', () => {
  if (shouldLock && document.visibilityState === 'visible') {
    request();
  }
});
