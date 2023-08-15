import { h, VNode } from 'snabbdom';

export default function userLink(u: string, title?: string, patron?: boolean): VNode {
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
    title && title != 'BOT' ? [line, h('span.utitle', title), u] : [line, u],
  );
}
