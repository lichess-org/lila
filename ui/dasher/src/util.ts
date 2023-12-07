import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { Redraw } from 'common/snabbdom';

export type Close = () => void;

// TODO nope
export interface Prop<T> {
  (): T;
  (v: T): T;
}

export function defined<A>(v: A | undefined): v is A {
  return typeof v !== 'undefined';
}

// like mithril prop but with type safety
// TODO nope
export function prop<A>(initialValue: A): Prop<A> {
  let value = initialValue;
  const fun = function (v: A | undefined) {
    if (typeof v !== 'undefined') value = v;
    return value;
  };
  return fun as Prop<A>;
}

export function bind(eventName: string, f: (e: Event) => void, redraw: Redraw | undefined = undefined) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        e.stopPropagation();
        f(e);
        if (redraw) redraw();
        return false;
      });
    },
  };
}

export function header(name: string, close: Close) {
  return h(
    'button.head.text',
    {
      attrs: { 'data-icon': licon.LessThan, type: 'button' },
      hook: bind('click', close),
    },
    name,
  );
}
