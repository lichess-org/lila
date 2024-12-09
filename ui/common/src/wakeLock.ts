let primerEvents = ['touchend', 'pointerup', 'pointerdown', 'mousedown', 'keydown'];
let wakeLock: WakeLockSentinel | null = null;
let keepScreenAwake = false;

export function request(): void {
  keepScreenAwake = true;
  acquire();
}

export function release(): void {
  keepScreenAwake = false;
  wakeLock?.release().catch(() => {});
  wakeLock = null;
}

function primer() {
  if (!wakeLock && keepScreenAwake) acquire();
}

function acquire() {
  navigator.wakeLock
    ?.request('screen')
    .then(sentinel => {
      wakeLock = sentinel;
      primerEvents.forEach(e => window.removeEventListener(e, primer, { capture: true }));
      primerEvents = [];
    })
    .catch(() => (wakeLock = null));
}

// safari will only grant wakeLock on a user interaction, and without that initial grant
// visibilitychange events won't affect it either
primerEvents.forEach(e => window.addEventListener(e, primer, { capture: true }));
document.addEventListener('visibilitychange', () => {
  if (keepScreenAwake && (!wakeLock || wakeLock.released) && document.visibilityState === 'visible') {
    acquire();
  } else if (document.visibilityState === 'hidden') {
    wakeLock?.release().catch(() => {});
    wakeLock = null;
  }
});
