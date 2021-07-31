import '@types/dom-screen-wake-lock';

let wakeLock: WakeLockSentinel | null = null;

export const request = () => {
  if ('wakeLock' in navigator) {
    navigator.wakeLock
      .request('screen')
      .then(sentinel => {
        wakeLock = sentinel;
      })
      .catch((error: Error) => {
        console.error('wakeLock - request failed: ' + error.message);
      });
  }
};

export const release = () =>
  wakeLock
    ?.release()
    .then(() => {
      wakeLock = null;
    })
    .catch((error: Error) => {
      console.error('wakeLock - release failed: ' + error.message);
    });

// re-request wakeLock if user switches tabs
document.addEventListener('visibilitychange', () => {
  if (wakeLock && document.visibilityState === 'visible') request();
});
