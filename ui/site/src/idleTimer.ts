export default function idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void) {
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
