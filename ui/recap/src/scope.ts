import { easeOutExpo, fmt } from './html';

// per-slide scope: timers and animation loops are cancelled automatically when the slide leaves
export interface Scope {
  after(ms: number, fn: () => void): void;
  // fn receives the elapsed ms; the loop stops when it returns false
  loop(fn: (t: number) => boolean | void): void;
  onDispose(fn: () => void): void;
  dispose(): void;
}

export function makeScope(): Scope {
  const timers: Timeout[] = [];
  const cleanups: (() => void)[] = [];
  let alive = true;
  return {
    after(ms, fn) {
      timers.push(setTimeout(() => alive && fn(), ms));
    },
    loop(fn) {
      const t0 = performance.now();
      const tick = (now: number) => {
        if (alive && fn(now - t0) !== false) requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    },
    onDispose(fn) {
      cleanups.push(fn);
    },
    dispose() {
      alive = false;
      timers.forEach(clearTimeout);
      cleanups.forEach(f => f());
    },
  };
}

interface CountOpts {
  dur?: number;
  delay?: number;
  format?: (n: number) => string;
}

export function countUp(
  sc: Scope,
  el: Element | null,
  to: number,
  { dur = 2200, delay = 0, format = fmt }: CountOpts = {},
): void {
  if (!el) return;
  el.textContent = format(0);
  sc.after(delay, () =>
    sc.loop(t => {
      const k = easeOutExpo(Math.min(1, t / dur));
      el.textContent = format(to * k);
      return k < 1;
    }),
  );
}

// animates every [data-count] element, honouring its data-delay (in seconds)
export const countAll = (sc: Scope, root: Element, opts: CountOpts = {}): void =>
  root.querySelectorAll<HTMLElement>('[data-count]').forEach(el =>
    countUp(sc, el, parseFloat(el.dataset.count!), {
      ...opts,
      delay: parseFloat(el.dataset.delay || '0') * 1000,
    }),
  );

export function typeText(sc: Scope, el: HTMLElement, text: string, delay = 0): void {
  sc.after(delay, () => {
    let shown = 0;
    sc.loop(t => {
      const n = Math.min(text.length, Math.floor(t / 38));
      if (n !== shown) el.textContent = text.slice(0, (shown = n));
      if (n < text.length) return true;
      sc.after(1600, () => el.classList.remove('typed'));
      return false;
    });
  });
}
