import { Attrs, h, VNode } from 'snabbdom';

export interface HasRating {
  rating?: number;
  provisional?: boolean;
}

export interface HasFlair {
  flair?: Flair;
}

export interface HasTitle {
  title?: string;
}

export interface AnyUser extends HasRating, HasFlair, HasTitle {
  name: string;
  line?: boolean; // display i.line, true by default
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
        ulpt: u.name != 'ghost',
        online: !!u.online,
      },
      attrs: {
        href: `/@/${u.name}`,
        ...u.attrs,
      },
    },
    [userLine(u), ...fullName(u), userRating(u)],
  );

export const userFlair = (u: HasFlair): VNode | undefined =>
  u.flair
    ? h('img.uflair', {
        attrs: {
          src: lichess.flairSrc(u.flair),
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

export const userTitle = (u: HasTitle): VNode | undefined =>
  u.title
    ? h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, [u.title, '\xa0'])
    : undefined;

export const fullName = (u: AnyUser) => [userTitle(u), u.name, userFlair(u)];

export const userRating = (u: HasRating): string | undefined =>
  u.rating ? ` (${u.rating + (u.provisional ? '?' : '')})` : undefined;
