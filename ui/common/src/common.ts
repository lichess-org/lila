export const defined = <T>(value: T | undefined): value is T => value !== undefined;

export const notNull = <T>(value: T | null | undefined): value is T => value !== null && value !== undefined;

export const isEmpty = <T>(a: T[] | undefined): boolean => !a || a.length === 0;

export const notEmpty = <T>(a: T[] | undefined): boolean => !isEmpty(a);

export function as<T>(v: T, f: () => void): () => T {
  return () => {
    f();
    return v;
  };
}

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

export const readonlyProp = <A>(initialValue: A): Prop<A> => {
  const value = initialValue;
  return () => value;
};

// Checking that the prop doesn't take an argument
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/length
export const isReadonlyProp = <A>(prop: Prop<A>) => prop.length === 0;

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

export const scrollToInnerSelector = (el: HTMLElement, selector: string, horiz: boolean = false) =>
  scrollTo(el, el.querySelector(selector), horiz);

export const scrollTo = (el: HTMLElement, target: HTMLElement | null, horiz: boolean = false) => {
  if (target)
    horiz
      ? (el.scrollLeft = target.offsetLeft - el.offsetWidth / 2 + target.offsetWidth / 2)
      : (el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2);
};

export const onClickAway = (f: () => void) => (el: HTMLElement) => {
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

export type SparseSet<T> = Set<T> | T;
export type SparseMap<V> = Map<string, SparseSet<V>>;

export function spread<T>(v: undefined | SparseSet<T>): T[] {
  return v === undefined ? [] : v instanceof Set ? [...v] : [v];
}

export function spreadMap<T>(m: SparseMap<T>): [string, T[]][] {
  return [...m].map(([k, v]) => [k, spread(v)]);
}

export function getSpread<T>(m: SparseMap<T>, key: string): T[] {
  return spread(m.get(key));
}

export function remove<T>(m: SparseMap<T>, key: string, val: T) {
  const v = m.get(key);
  if (v === val) m.delete(key);
  else if (v instanceof Set) v.delete(val);
}

export function pushMap<T>(m: SparseMap<T>, key: string, val: T) {
  const v = m.get(key);
  if (!v) m.set(key, val);
  else {
    if (v instanceof Set) v.add(val);
    else if (v !== val) m.set(key, new Set([v as T, val]));
  }
}
