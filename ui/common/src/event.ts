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
