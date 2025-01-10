export const defined = <T>(value: T | undefined): value is T => value !== undefined;

export const notNull = <T>(value: T | null | undefined): value is T =>
  value !== null && value !== undefined;

export const isEmpty = <T>(a: T[] | undefined): boolean => !a || a.length === 0;

export const notEmpty = <T>(a: T[] | undefined): boolean => !isEmpty(a);

export interface Prop<T> {
  (): T;
  (v: T): T;
}

// like mithril prop but with type safety
export const prop = <A>(initialValue: A): Prop<A> => {
  let value = initialValue;
  const fun = (v: A | undefined) => {
    if (defined(v)) value = v;
    return value;
  };
  return fun as Prop<A>;
};

// It's possible multiple seconds will elapse before the callback is fired
export const requestIdleCallbackWithFallback = (f: () => void, timeout?: number): void => {
  if (window.requestIdleCallback) window.requestIdleCallback(f, timeout ? { timeout } : undefined);
  else requestAnimationFrame(f);
};

// use memoize
export const useJp = (): boolean => document.documentElement.lang === 'ja';
