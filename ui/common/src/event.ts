// stale listeners can cause memory loss, dry skin, and tooth decay

export interface EventJanitor {
  addListener: <T extends EventTarget, E extends Event>(
    target: T,
    type: string,
    listener: (this: T, ev: E) => any,
    options?: boolean | AddEventListenerOptions,
  ) => void;
  removeAll: () => void;
}

export function eventJanitor(): EventJanitor {
  const removers: (() => void)[] = [];

  return {
    addListener: <T extends EventTarget, E extends Event>(
      target: T,
      type: string,
      listener: (this: T, ev: E) => any,
      options?: boolean | AddEventListenerOptions,
    ): void => {
      target.addEventListener(type, listener, options);
      removers.push(() => target.removeEventListener(type, listener, options));
    },
    removeAll: () => {
      removers.forEach(r => r());
      removers.length = 0;
    },
  };
}
