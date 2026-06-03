import debounce from 'debounce-promise';

import * as xhr from '@/xhr';

import { complete } from './complete';

export interface UserCompleteResult {
  result: LightUserOnline[];
}

export interface UserCompleteOpts {
  input: HTMLInputElement;
  tag?: 'a' | 'span';
  minLength?: number;
  populate?: (result: LightUserOnline) => string;
  onSelect?: (result: LightUserOnline) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
  team?: string;
}

export function userComplete(opts: UserCompleteOpts): void {
  const debouncedXhr = debounce((t: string) => fetchUsers(t, opts), 150);

  complete<LightUserOnline>({
    input: opts.input,
    fetch: t => debouncedXhr(t).then(checkDebouncedResultAgainstTerm(t)),
    render: (o: LightUserOnline) => renderUserEntry(o, opts.tag),
    populate: opts.populate || (r => r.name),
    onSelect: opts.onSelect,
    regex: /^[a-z][\w-]{2,29}$/i,
  });
  if (opts.focus) setTimeout(() => opts.input.focus());
}

type ResultOfTerm = { term: string } & UserCompleteResult;

export const fetchUsers = async (
  term: string,
  { friend, tour, swiss, team }: Partial<UserCompleteOpts>,
): Promise<ResultOfTerm> => {
  const result = await xhr.json(
    xhr.url('/api/player/autocomplete', {
      term,
      friend: friend ? 1 : 0,
      tour,
      swiss,
      team,
      object: 1,
    }),
  );
  return { term, ...result };
};

export const checkDebouncedResultAgainstTerm =
  (term: string) =>
  (got: ResultOfTerm): Promise<LightUserOnline[]> =>
    term === got.term ? Promise.resolve(got.result) : Promise.reject(new Error('Debounced ' + term));

export const renderUserEntry = (o: LightUserOnline, tag = 'a'): string => {
  const patronClass = o.patronColor ? ` paco${o.patronColor}` : '';
  const hrefAttr = tag === 'a' ? 'href' : 'data-href';
  const title = o.title
    ? `<span class="utitle"${o.title === 'BOT' ? ' data-bot="data-bot"' : ''}>${o.title}</span>&nbsp;`
    : '';
  const flair = o.flair ? `<img class="uflair" src="${site.asset.flairSrc(o.flair)}" alt="" />` : '';
  return `<${tag} class="complete-result ulpt user-link${o.online ? ' online' : ''}" ${hrefAttr}="/@/${o.name}"><icon class="line${o.patron ? ' patron' : ''}${patronClass}"></icon>${title}${o.name}${flair}</${tag}>`;
};
