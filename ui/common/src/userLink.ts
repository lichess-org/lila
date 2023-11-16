import { h, VNode } from 'snabbdom';

export default function userLink(u: string, title?: string, patron?: boolean, flair?: Flair): VNode {
  const line = patron
    ? h('line.line.patron', {
        attrs: {
          title: 'Lichess Patron',
        },
      })
    : undefined;
  const flairImg = userFlair(flair);
  return h(
    'a',
    {
      // can't be inlined because of thunks
      class: {
        'user-link': true,
        ulpt: true,
      },
      attrs: {
        href: `/@/${u}`,
      },
    },
    title && title != 'BOT' ? [line, h('span.utitle', title), u, flairImg] : [line, u, flairImg],
  );
}

export const userFlair = (flair?: Flair): VNode | undefined =>
  flair
    ? h('img.uflair', {
        attrs: {
          src: lichess.assetUrl(`lifat/flair/img/${flair}.webp`, { noVersion: true }),
        },
      })
    : undefined;
