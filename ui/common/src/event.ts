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

export function browserTaskQueueMonitor(interval = 1000): { wasSuspended: boolean; reset: () => void } {
  let lastTime: number;
  let timeout: Timeout;
  let suspended = false;

  start();

  return {
    get wasSuspended() {
      return suspended;
    },
    reset() {
      suspended = false;
      clearTimeout(timeout);
      start();
    },
  };

  function monitor() {
    if (performance.now() - lastTime > interval + 400) suspended = true;
    else start();
  }

  function start() {
    lastTime = performance.now();
    timeout = setTimeout(monitor, interval);
  }
}

export class Janitor {
  private cleanupTasks: (() => void)[] = [];

  addListener<T extends EventTarget, E extends Event>(
    target: T,
    type: string,
    listener: (this: T, ev: E) => any,
    options?: boolean | AddEventListenerOptions,
  ): void {
    target.addEventListener(type, listener, options);
    this.cleanupTasks.push(() => target.removeEventListener(type, listener, options));
  }
  addCleanupTask(task: () => void): void {
    this.cleanupTasks.push(task);
  }
  cleanup(): void {
    for (const task of this.cleanupTasks) task();
    this.cleanupTasks.length = 0;
  }
}
