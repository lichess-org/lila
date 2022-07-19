import { h } from 'snabbdom';

export function userLink(u: string, title?: string) {
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
    title ? [h('span.title', title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, title), trunc] : [trunc]
  );
}
