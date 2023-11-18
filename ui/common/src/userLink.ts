import { h, VNode } from 'snabbdom';

export interface AnyUser {
  name: string;
  title?: string;
  rating?: number;
  flair?: Flair;
  patron?: boolean;
}

export default function userLink(u: AnyUser): VNode {
  const line = u.patron ? h('i.line.patron', { attrs: { title: 'Lichess Patron' } }) : undefined;
  const titleSpan =
    u.title && h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title);
  const flairImg = userFlair(u.flair);
  const ratingText = u.rating && ` (${u.rating})`;
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
