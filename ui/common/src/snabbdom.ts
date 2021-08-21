import type { VNode, Hooks, Attrs } from 'snabbdom';

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void, passive = true): Hooks {
  return onInsert(el =>
    el.addEventListener(
      eventName,
      e => {
        const res = f(e);
        if (res === false) e.preventDefault();
        redraw?.();
        return res;
      },
      { passive }
    )
  );
}

export const bindNonPassive = (eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks =>
  bind(eventName, f, redraw, false);

export function bindSubmit(f: (e: Event) => unknown, redraw?: () => void): Hooks {
  return bind('submit', e => (e.preventDefault(), f(e)), redraw, false);
}

export function bindMobileMousedown(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void): void {
  for (const mousedownEvent of ['touchstart', 'mousedown']) {
    el.addEventListener(
      mousedownEvent,
      e => {
        f(e);
        e.preventDefault();
        if (redraw) redraw();
      },
      { passive: false }
    );
  }
}

export function dataIcon(icon: string): Attrs {
  return {
    'data-icon': icon,
  };
}
