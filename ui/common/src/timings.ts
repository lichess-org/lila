export function idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): void {
  const events = ['mousemove', 'touchstart'];

  let listening = false,
    active = true,
    lastSeenActive = performance.now();

  const onActivity = () => {
    if (!active) {
      // console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = performance.now();
    stopListening();
  };

  const startListening = () => {
    if (!listening) {
      events.forEach(e => document.addEventListener(e, onActivity));
      listening = true;
    }
  };

  const stopListening = () => {
    if (listening) {
      events.forEach(e => document.removeEventListener(e, onActivity));
      listening = false;
    }
  };

  setInterval(() => {
    if (active && performance.now() - lastSeenActive > delay) {
      // console.log('Idle mode');
      onIdle();
      active = false;
    }
    startListening();
  }, 10000);
}

export function debounce<T extends (...args: any) => any>(
  f: T,
  wait: number,
  immediate = false
): (...args: Parameters<T>) => any {
  let timeout: Timeout | undefined;
  let lastBounce = 0;

  return function (this: any, ...args: Parameters<T>) {
    const self = this;

    if (timeout) clearTimeout(timeout);
    timeout = undefined;

    const elapsed = performance.now() - lastBounce;
    lastBounce = performance.now();
    if (immediate && elapsed > wait) f.apply(self, args);
    else
      timeout = setTimeout(() => {
        timeout = undefined;
        f.apply(self, args);
      }, wait);
  };
}
