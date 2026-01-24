// no side effects allowed due to re-export by index.ts

export const defined = <T>(value: T | undefined): value is T => value !== undefined;

export const notNull = <T>(value: T | null | undefined): value is T => value !== null && value !== undefined;

export const isEmpty = <T>(a: T[] | undefined): boolean => !a || a.length === 0;

export const notEmpty = <T>(a: T[] | undefined): boolean => !isEmpty(a);

export interface Prop<T> {
  (): T;
  (v: T): T;
}
export interface PropWithEffect<T> extends Prop<T> {}

// like mithril prop but with type safety
export const prop = <A>(initialValue: A): Prop<A> => {
  let value = initialValue;
  return (v?: A) => {
    if (defined(v)) value = v;
    return value;
  };
};

export const propWithEffect = <A>(initialValue: A, effect: (value: A) => void): PropWithEffect<A> => {
  let value = initialValue;
  return (v?: A) => {
    if (defined(v)) {
      value = v;
      effect(v);
    }
    return value;
  };
};

export const withEffect =
  <T>(prop: Prop<T>, effect: (v: T) => void): PropWithEffect<T> =>
  (v?: T) => {
    let returnValue;
    if (defined(v)) {
      returnValue = prop(v);
      effect(v);
    } else returnValue = prop();
    return returnValue;
  };

export interface Toggle extends PropWithEffect<boolean> {
  toggle(): void;
  effect(value: boolean): void;
}

export const toggle = (initialValue: boolean, effect: (value: boolean) => void = () => {}): Toggle => {
  const prop = propWithEffect<boolean>(initialValue, effect) as Toggle;
  prop.toggle = () => prop(!prop());
  return prop;
};

export const toggleWithConstraint = (value: boolean, constraint: () => boolean): Toggle => {
  return Object.assign(
    (v?: boolean): boolean => {
      if (defined(v)) value = v && constraint();
      return value;
    },
    { toggle: () => (value = !value && constraint()), effect: () => {} },
  );
};

// Only computes a value once. The computed value must not be undefined.
export const memoize = <A>(compute: () => A): (() => A) => {
  let computed: A;
  return () => {
    if (computed === undefined) computed = compute();
    return computed;
  };
};

export const scrollToInnerSelector = (
  el: HTMLElement,
  selector: string,
  horiz: boolean = false,
  behavior: ScrollBehavior = 'instant',
): void => scrollTo(el, el.querySelector(selector), horiz, behavior);

export const scrollTo = (
  el: HTMLElement,
  target: HTMLElement | null,
  horiz: boolean = false,
  behavior: ScrollBehavior = 'instant',
): void => {
  if (!target) return;
  el.scrollTo(
    horiz
      ? { behavior, left: target.offsetLeft - el.offsetWidth / 2 + target.offsetWidth / 2 }
      : { behavior, top: target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2 },
  );
};

export const onClickAway =
  (f: () => void) =>
  (el: HTMLElement): void => {
    const listen: () => void = () =>
      $(document).one('click', e => {
        if (!document.contains(el)) return;
        if (el.contains(e.target)) listen();
        else f();
      });
    setTimeout(listen, 300);
  };

export function hyphenToCamel(str: string): string {
  return str.replace(/-([a-z])/g, g => g[1].toUpperCase());
}

export const requestIdleCallback = (f: () => void, timeout?: number): void => {
  if (window.requestIdleCallback) window.requestIdleCallback(f, timeout ? { timeout } : undefined);
  else requestAnimationFrame(f);
};

export function escapeHtml(str: string): string {
  if (typeof str !== 'string') str = JSON.stringify(str); // throws
  return /[&<>"']/.test(str)
    ? str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
    : str;
}

export function frag<T extends Node = Node>(html: string): T {
  const fragment = document.createRange().createContextualFragment(html);
  return (fragment.childElementCount === 1 ? fragment.firstElementChild : fragment) as unknown as T;
}

export function scopedQuery(scope: Element): <T extends Element = HTMLElement>(sel: string) => T | null {
  return <T extends Element = HTMLElement>(sel: string) => scope.querySelector<T>(sel);
}

// The username with all characters lowercase
export function myUserId(): string | undefined {
  return document.body.dataset.user;
}

export function myUsername(): string | undefined {
  return document.body.dataset.username;
}

export function repeater(f: () => void, additionalStopCond?: () => boolean): void {
  let timeout: number | undefined = undefined;
  const delay = (function* () {
    yield 500;
    for (let d = 350; ; ) yield Math.max(100, (d *= 14 / 15));
  })();
  const repeat = () => {
    f();
    timeout = setTimeout(repeat, delay.next().value!);
    if (additionalStopCond?.()) clearTimeout(timeout);
  };
  repeat();
  document.addEventListener('pointerup', () => clearTimeout(timeout), { once: true });
}
