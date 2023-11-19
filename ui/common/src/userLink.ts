import { Attrs, h, VNode } from 'snabbdom';

export interface AnyUser {
  name: string;
  title?: string;
  rating?: number;
  provisional?: boolean;
  flair?: Flair;
  line?: boolean; // display i.line
  patron?: boolean; // turn i.line into a patron wing
  moderator?: boolean; // turn i.line into a mod icon
  online?: boolean; // light up .line
  attrs?: Attrs;
}

export const userLink = (u: AnyUser): VNode =>
  h(
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
    [userLine(u), ...fullName(u), userRating(u)],
  );

export const userFlair = (u: AnyUser): VNode | undefined =>
  u.flair
    ? h('img.uflair', {
        attrs: {
          src: lichess.assetUrl(`lifat/flair/img/${u.flair}.webp`, { noVersion: true }),
        },
      })
    : undefined;

export const userLine = (u: AnyUser): VNode | undefined =>
  u.line !== false
    ? h('i.line', {
        class: { patron: !!u.patron, moderator: !!u.moderator },
        attrs: u.patron ? { title: 'Lichess Patron' } : {},
      })
    : undefined;

export const userTitle = (u: AnyUser): VNode | undefined =>
  u.title
    ? h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, [u.title, '\xa0'])
    : undefined;

export const fullName = (u: AnyUser) => [userTitle(u), u.name, userFlair(u)];

export const userRating = (u: AnyUser): string | undefined =>
  u.rating ? ` (${u.rating + (u.provisional ? '?' : '')})` : undefined;
