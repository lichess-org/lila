import { h, VNode } from 'snabbdom';

export function userLinkColor(u: string, title?: string, patron?: boolean,owner?: boolean): VNode {
  const trunc = u.length > 13 ? u.substring(0, 13) + '…' : u;
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
        'simul-host': owner?true:false,
        
      },
      attrs: {
        href: '/@/' + u,
      },
    },
    title && title != 'BOT' ? [line, h('span.utitle', title), trunc] : [line, trunc]
  );

}

export function userLink(u: string, title?: string, patron?: boolean): VNode {
  return userLinkColor(u,title,patron,undefined)
}

export function bind(eventName: string, f: (e: Event) => void) {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, f);
    },
  };
}
