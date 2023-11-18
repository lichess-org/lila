import { h, VNode } from 'snabbdom';

export interface AnyUser {
  name: string;
  title?: string;
  rating?: number;
  flair?: Flair;
  patron?: boolean;
}

export default function userLink(u: AnyUser): VNode {
  const line = u.patron ? h('line.line.patron', { attrs: { title: 'Lichess Patron' } }) : undefined;
  const titleSpan = u.title && u.title != 'BOT' ? h('span.utitle', u.title) : undefined;
  const flairImg = userFlair(u.flair);
  const ratingText = u.rating ? ` (${u.rating})` : undefined;
  return h(
    'a',
    {
      // can't be inlined because of thunks
      class: {
        'user-link': true,
        ulpt: true,
      },
      attrs: {
        href: `/@/${u.name}`,
      },
    },
    [line, titleSpan, u.name, flairImg, ratingText],
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
