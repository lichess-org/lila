import * as xhr from '@/xhr';
import debounce from 'debounce-promise';
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
export const fetchUsers = async (term: string, opts: Partial<UserCompleteOpts>): Promise<ResultOfTerm> => {
  const result = await xhr.json(
    xhr.url('/api/player/autocomplete', {
      term,
      friend: opts.friend ? 1 : 0,
      tour: opts.tour,
      swiss: opts.swiss,
      team: opts.team,
      object: 1,
    }),
  );
  return { term, ...result };
};

export const checkDebouncedResultAgainstTerm =
  (term: string) =>
  (got: ResultOfTerm): Promise<LightUserOnline[]> =>
    term === got.term ? Promise.resolve(got.result) : Promise.reject('Debounced ' + term);

export const renderUserEntry = (o: LightUserOnline, tag: string = 'a'): string => {
  const patronClass = o.patronColor ? ` paco${o.patronColor}` : '';
  return (
    '<' +
    tag +
    ' class="complete-result ulpt user-link' +
    (o.online ? ' online' : '') +
    '" ' +
    (tag === 'a' ? '' : 'data-') +
    'href="/@/' +
    o.name +
    '">' +
    '<i class="line' +
    (o.patron ? ' patron' : '') +
    patronClass +
    '"></i>' +
    (o.title
      ? '<span class="utitle"' +
        (o.title === 'BOT' ? ' data-bot="data-bot" ' : '') +
        '>' +
        o.title +
        '</span>&nbsp;'
      : '') +
    o.name +
    (o.flair ? '<img class="uflair" src="' + site.asset.flairSrc(o.flair) + '"/>' : '') +
    '</' +
    tag +
    '>'
  );
};
