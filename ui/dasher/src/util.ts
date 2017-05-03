import { h } from 'snabbdom'

export type Redraw = () => void

export interface Prop<T> {
  (): T
  (v: T): T
}

export function defined(v: any): boolean {
  return typeof v !== 'undefined';
}

// like mithril prop but with type safety
export function prop<A>(initialValue: A): Prop<A> {
  let value = initialValue;
  const fun = function (v: A | undefined) {
    if (typeof v !== 'undefined') value = v;
    return value;
  };
  return fun as Prop<A>;
};

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
