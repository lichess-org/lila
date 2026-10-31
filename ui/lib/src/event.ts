export function idleTimer(delay: number, onIdle: () => void, onWakeUp: () => void): () => void {
  const events = ['mousemove', 'touchstart'];

  let lastSeenActive: number | undefined = performance.now();

  const onActivity = () => {
    if (interval === undefined) return;
    if (lastSeenActive === undefined) {
      // console.log('Wake up');
      onWakeUp();
    }
    lastSeenActive = performance.now();
    stopListening();
  };

  const stopListening = () => {
    events.forEach(e => document.removeEventListener(e, onActivity));
  };

  let interval: ReturnType<typeof setInterval> | undefined = setInterval(() => {
    if (interval === undefined) return;
    if (lastSeenActive !== undefined && performance.now() - lastSeenActive > delay) {
      // console.log('Idle mode');
      lastSeenActive = undefined;
      onIdle();
    }
    if (interval !== undefined) events.forEach(e => document.addEventListener(e, onActivity));
  }, 10000);

  return () => {
    clearInterval(interval);
    interval = undefined;
    stopListening();
  };
}

export class Reaper {
  private cleanupTasks?: (() => void)[] = [];

  addListener<T extends EventTarget, E extends Event>(
    target: T,
    type: string,
    listener: (this: T, ev: E) => any,
    options?: boolean | AddEventListenerOptions,
  ): void {
    target.addEventListener(type, listener, options);
    this.addCleanup(() => target.removeEventListener(type, listener, options));
  }

  addCleanup(...task: (() => void)[]): void {
    if (!this.cleanupTasks) task.forEach(t => t());
    else this.cleanupTasks.push(...task);
  }

  dispose(): void {
    const tasks = this.cleanupTasks;
    this.cleanupTasks = undefined;
    for (const task of tasks ?? []) task();
  }

  get disposed(): boolean {
    return this.cleanupTasks === undefined;
  }

  cleanup(): void {
    for (const task of this.cleanupTasks?.splice(0) ?? []) task();
  }
}
