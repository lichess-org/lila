import { Attrs, h, VNode } from 'snabbdom';

export interface HasRating {
  rating?: number;
  provisional?: boolean;
  brackets?: boolean; // display the rating in brackets/parentheses, true by default
}

export interface HasRatingDiff {
  ratingDiff?: number;
}

export interface HasFlair {
  flair?: Flair;
}

export interface HasTitle {
  title?: string;
}

export interface HasLine {
  line?: boolean; // display i.line, true by default
  patron?: boolean; // turn i.line into a patron wing
  moderator?: boolean; // turn i.line into a mod icon
}

export interface AnyUser extends HasRating, HasFlair, HasTitle, HasLine {
  name: string;
  online?: boolean; // light up .line
  attrs?: Attrs;
}

export const userLink = (u: AnyUser): VNode =>
  h(
    'a',
    {
      // can't be inlined because of thunks
      class: { 'user-link': true, ulpt: u.name != 'ghost', online: !!u.online },
      attrs: { href: `/@/${u.name}`, ...u.attrs },
    },
    [userLine(u), ...fullName(u), u.rating && ` ${userRating(u)} `],
  );

export const userFlair = (u: HasFlair): VNode | undefined =>
  u.flair ? h('img.uflair', { attrs: { src: lichess.asset.flairSrc(u.flair) } }) : undefined;

export const userLine = (u: HasLine): VNode | undefined =>
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

export const userRating = (u: HasRating): string | undefined => {
  if (u.rating) {
    const rating = `${u.rating}${u.provisional ? '?' : ''}`;
    return u.brackets !== false ? `(${rating})` : rating;
  }
  return undefined;
};

export const ratingDiff = (u: HasRatingDiff): VNode | undefined =>
  u.ratingDiff === 0
    ? h('span', '±0')
    : u.ratingDiff && u.ratingDiff > 0
    ? h('good', '+' + u.ratingDiff)
    : u.ratingDiff && u.ratingDiff < 0
    ? h('bad', '−' + -u.ratingDiff)
    : undefined;
