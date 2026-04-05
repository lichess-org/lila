import { type Attrs, h, type VNode, type VNodeData } from 'snabbdom';

import { type MaybeVNodes } from './snabbdom';

export type AnyUser = {
  name: string;
  online?: boolean; // light up .line
  playing?: boolean; // light up .line in red
  attrs?: Attrs;
  title?: string;
  flair?: Flair;
  ratingDiff?: number;
  line?: boolean; // display i.line, true by default
  patronColor?: PatronColor; // turn i.line into a patron wing
  moderator?: boolean; // turn i.line into a mod icon
  rating?: number;
  provisional?: boolean;
  brackets?: boolean; // display the rating in brackets/parentheses, true by default
};

export const userLink = (u: AnyUser): VNode =>
  h('a', userLinkData(u), [userLine(u), ...fullName(u), u.rating && ` ${userRating(u)} `]);

export const userLinkData = (u: AnyUser): VNodeData => ({
  // can't be inlined because of thunks
  class: { 'user-link': true, ulpt: u.name !== 'ghost', online: !!u.online, playing: !!u.playing },
  attrs: { href: `/@/${u.name}`, ...u.attrs },
});

export const userFlair = (u: Pick<AnyUser, 'flair'>): VNode | undefined =>
  u.flair ? h('img.uflair', { attrs: { src: site.asset.flairSrc(u.flair) } }) : undefined;

export const userLine = (u: Pick<AnyUser, 'line' | 'patronColor' | 'moderator'>): VNode | undefined =>
  u.line !== false
    ? h('i.line', {
        class: {
          patron: !!u.patronColor,
          moderator: !!u.moderator,
          ...(u.patronColor ? { [`paco${u.patronColor}`]: true } : {}),
        },
        attrs: u.patronColor ? { title: 'Lichess Patron' } : {},
      })
    : undefined;

export const userTitle = ({ title }: Pick<AnyUser, 'title'>): VNode | undefined =>
  title
    ? h('span.utitle', title === 'BOT' ? { attrs: { 'data-bot': true } } : {}, [title, '\xa0'])
    : undefined;

export const fullName = (u: AnyUser): MaybeVNodes => [userTitle(u), u.name, userFlair(u)];

export const userRating = (u: Pick<AnyUser, 'rating' | 'provisional' | 'brackets'>): string | undefined => {
  if (u.rating) {
    const rating = `${u.rating}${u.provisional ? '?' : ''}`;
    return u.brackets !== false ? `(${rating})` : rating;
  }
  return undefined;
};

export const ratingDiff = ({ ratingDiff }: Pick<AnyUser, 'ratingDiff'>): VNode | undefined =>
  ratingDiff === 0
    ? h('span', '±0')
    : ratingDiff && ratingDiff > 0
      ? h('good', '+' + ratingDiff)
      : ratingDiff && ratingDiff < 0
        ? h('bad', '−' + -ratingDiff)
        : undefined;
