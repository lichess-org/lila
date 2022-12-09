import { h, VNode } from 'snabbdom';

export type Redraw = () => void;
export type Close = () => void;
export type Open = (sub: string) => void;

export interface Prop<T> {
  (): T;
  (v: T): T;
}

export function defined<A>(v: A | undefined): v is A {
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
  let icon = ''; // <
  if (document.dir == 'rtl') {
    icon = ''; // >
  }

  return h(
    'button.head.text',
    {
      attrs: { 'data-icon': icon, type: 'button' },
      hook: bind('click', close),
    },
    name
  );
}
