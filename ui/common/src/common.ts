export function defined<A>(v: A | undefined): v is A {
  return typeof v !== 'undefined';
}

export function notNull<T>(value: T | null | undefined): value is T {
  return value !== null && value !== undefined;
}

export function isEmpty(a: any): boolean {
  return !a || a.length === 0;
}

export interface Prop<T> {
  (): T
  (v: T): T
}

// like mithril prop but with type safety
export function prop<A>(initialValue: A): Prop<A> {
  let value = initialValue;
  const fun = function(v: A | undefined) {
    if (defined(v)) value = v;
    return value;
  };
  return fun as Prop<A>;
}
