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
  prop.effect = effect;
  return prop;
};

// Only computes a value once. The computed value must not be undefined.
export const memoize = <A>(compute: () => A): (() => A) => {
  let computed: A;
  return () => {
    if (computed === undefined) computed = compute();
    return computed;
  };
};

export const scrollToInnerSelector = (el: HTMLElement, selector: string, horiz: boolean = false): void =>
  scrollTo(el, el.querySelector(selector), horiz);

export const scrollTo = (el: HTMLElement, target: HTMLElement | null, horiz: boolean = false): void => {
  if (target)
    horiz
      ? (el.scrollLeft = target.offsetLeft - el.offsetWidth / 2 + target.offsetWidth / 2)
      : (el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2);
};

export const onClickAway =
  (f: () => void) =>
  (el: HTMLElement): void => {
    const listen: () => void = () =>
      $(document).one('click', e => {
        if (!document.contains(el)) {
          return;
        }
        if (el.contains(e.target)) {
          listen();
        } else {
          f();
        }
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

export const escapeHtml = (str: string): string =>
  /[&<>"']/.test(str)
    ? str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
    : str;

export const clamp = (value: number, bounds: { min?: number; max?: number }): number =>
  Math.max(bounds.min ?? -Infinity, Math.min(value, bounds.max ?? Infinity));

export function shuffle<T>(array: T[]): T[] {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

export function deepFreeze<T>(obj: T): T {
  if (obj !== null && typeof obj === 'object')
    Object.values(obj)
      .filter(v => v !== null && typeof v === 'object')
      .forEach(o => deepFreeze(o));
  return Object.freeze(obj);
}

// recursive comparison of enumerable primitives. complex properties get reference equality only
export function isEquivalent(a: any, b: any): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (Array.isArray(a)) {
    return Array.isArray(b) && a.length === b.length && a.every((x, i) => isEquivalent(x, b[i]));
  }
  if (typeof a !== 'object') return false;
  const [aKeys, bKeys] = [Object.keys(a), Object.keys(b)];
  if (aKeys.length !== bKeys.length) return false;
  for (const key of aKeys) {
    if (!bKeys.includes(key) || !isEquivalent(a[key], b[key])) return false;
  }
  return true;
}

export function zip<T, U>(arr1: T[], arr2: U[]): [T, U][] {
  const length = Math.min(arr1.length, arr2.length);
  const result: [T, U][] = [];
  for (let i = 0; i < length; i++) {
    result.push([arr1[i], arr2[i]]);
  }
  return result;
}

export function findMapped<T, U>(arr: T[], callback: (el: T) => U | undefined): U | undefined {
  for (const el of arr) {
    const result = callback(el);
    if (result) return result;
  }
  return undefined;
}

export function frag<T extends Node = Node>(html: string): T {
  const div = document.createElement('div');
  div.innerHTML = html;

  const fragment: DocumentFragment = document.createDocumentFragment();
  while (div.firstChild) fragment.appendChild(div.firstChild);

  return (fragment.childElementCount === 1 ? fragment.firstElementChild : fragment) as unknown as T;
}
