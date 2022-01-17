let wakeLock: WakeLockSentinel | null = null;

export const request = async () => {
  if ('wakeLock' in navigator)
    try {
      wakeLock = await navigator.wakeLock.request('screen');
    } catch (e) {
      console.error('wakeLock - request failed: ' + e);
    }
};

export const release = async () => {
  try {
    await wakeLock?.release();
    wakeLock = null;
  } catch (e) {
    console.error('wakeLock - release failed: ' + e);
  }
};

// re-request wakeLock if user switches tabs
document.addEventListener('visibilitychange', () => {
  if (wakeLock && document.visibilityState === 'visible') request();
});
