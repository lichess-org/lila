import { h, VNode } from 'snabbdom';

export function userLink(u: string, title?: string): VNode {
  const trunc = u.substring(0, 14);
  return h(
    'a',
    {
      // can't be inlined because of thunks
      class: {
        'user-link': true,
        ulpt: true,
      },
      attrs: {
        href: '/@/' + u,
      },
    },
    title && title != 'BOT' ? [h('span.utitle', title), trunc] : [trunc]
  );
}

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, f);
    },
  };
}
