import { h, VNode } from 'snabbdom';

export function userLink(u: string, title?: string, patron?: boolean): VNode {
  const trunc = u.substring(0, 14);
  const line = patron
    ? h('line.line.patron', {
        attrs: {
          title: 'Lichess Patron',
        },
      })
    : undefined;
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
    title && title != 'BOT' ? [line, h('span.utitle', title), trunc] : [line, trunc]
  );
}

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, f);
    },
  };
}
