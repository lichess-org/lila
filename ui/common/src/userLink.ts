import { Attrs, h, VNode } from 'snabbdom';

export interface AnyUser {
  name: string;
  title?: string;
  rating?: number;
  provisional?: boolean;
  flair?: Flair;
  line?: boolean; // display i.line
  patron?: boolean; // turn i.line into a patron wing
  online?: boolean; // light up .line
  attrs?: Attrs;
}

export default function userLink(u: AnyUser): VNode {
  const lineIcon = u.line
    ? h('i.line', { class: { patron: !!u.patron }, attrs: u.patron ? { title: 'Lichess Patron' } : {} })
    : undefined;
  const titleSpan =
    u.title && h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, [u.title, '\xa0']);
  const flairImg = userFlair(u.flair);
  const ratingText = u.rating && ` (${u.rating + (u.provisional ? '?' : '')})`;
  return h(
    'a',
    {
      // can't be inlined because of thunks
      class: {
        'user-link': true,
        ulpt: true,
        online: !!u.online,
      },
      attrs: {
        href: `/@/${u.name}`,
        ...u.attrs,
      },
    },
    [lineIcon, titleSpan, u.name, flairImg, ratingText],
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
