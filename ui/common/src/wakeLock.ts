const supported: boolean = 'wakeLock' in navigator;
let sentinel: WakeLockSentinel | null = null;

export const request = () => {
  if (supported) {
    navigator.wakeLock
      .request('screen')
      .then((response: WakeLockSentinel) => {
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
        sentinel = null;
      })
      .catch((error: Error) => {
        console.error('wakeLock - release failure. ' + error.message);
      });
  }
};

document.addEventListener('visibilitychange', () => {
  if (sentinel !== null && document.visibilityState === 'visible') {
    request();
  }
});
