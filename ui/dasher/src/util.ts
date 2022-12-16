import { VNode, h } from 'snabbdom';

export type Redraw = () => void;
export type Close = () => void;
export type Open = (sub: string) => void;

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
    'a.head.text',
    {
      attrs: { 'data-icon': 'I' },
      hook: bind('click', close),
    },
    name
  );
}

export function validateUrl(url: string): boolean {
  // modules/pref/src/main/PrefForm.scala
  return url === '' || ((url.startsWith('https://') || url.startsWith('//')) && url.length >= 10 && url.length <= 400);
}
