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
}

export const toggle = (initialValue: boolean, effect: (value: boolean) => void = () => {}): Toggle => {
  const prop = propWithEffect<boolean>(initialValue, effect) as Toggle;
  prop.toggle = () => prop(!prop());
  return prop;
};

export interface Selectable {
  select: () => void;
  deselect: () => void;
  destroy: () => void;
}

export class Selector<T extends Selectable> {
  // mutex group when cleanup matters
  select(name: string | false) {
    if (this.selectedName) {
      if (this.selectedName !== name) this.selected?.deselect();
      else return;
    }
    this.selectedName = name;
    this.selected?.select();
  }

  add(name: string, val: T) {
    this.clear(name);
    this.group.set(name, val);
    if (this.selectedName === name) val.select();
  }

  clear(name?: string) {
    if (!name) {
      for (const key of this.group.keys()) this.clear(key);
      this.selectedName = false;
      return;
    }
    if (this.selectedName === name) this.selectedName = false;
    this.group.get(name)?.deselect();
    this.group.get(name)?.destroy();
    this.group.delete(name);
  }
  get selected(): T | undefined {
    return this.selectedName ? this.group.get(this.selectedName) : undefined;
  }
  selectedName: string | false = false;
  group = new Map<string, T>();
}

// Only computes a value once. The computed value must not be undefined.
export const memoize = <A>(compute: () => A): (() => A) => {
  let computed: A;
  return () => {
    if (computed === undefined) computed = compute();
    return computed;
  };
};

export const scrollToInnerSelector = (el: HTMLElement, selector: string) => scrollTo(el, el.querySelector(selector));

export const scrollTo = (el: HTMLElement, target: HTMLElement | null) => {
  if (target) el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2;
};

export const onClickAway = (f: () => void) => (el: HTMLElement) => {
  const listen: () => void = () => $(document).one('click', e => (el.contains(e.target) ? listen() : f()));
  setTimeout(listen, 100);
};
