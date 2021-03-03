import { Hooks } from 'snabbdom/hooks';

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el =>
    el.addEventListener(eventName, e => {
      const res = f(e);
      if (redraw) redraw();
      return res;
    })
  );
}

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export const getNow = (): number => Math.round(performance.now());

export const uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];
