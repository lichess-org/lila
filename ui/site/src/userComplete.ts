import * as xhr from 'common/xhr';
import complete from 'common/complete';
import debounce from 'debounce-promise';

export interface Result {
  result: LightUserOnline[];
}

interface Opts {
  input: HTMLInputElement;
  tag?: 'a' | 'span';
  minLength?: number;
  populate?: (result: LightUserOnline) => string;
  onSelect?: (result: LightUserOnline) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
}

export default function (opts: Opts): void {
  const debounced = debounce(
    (term: string) =>
      xhr
        .json(
          xhr.url('/player/autocomplete', {
            term,
            friend: opts.friend ? 1 : 0,
            tour: opts.tour,
            swiss: opts.swiss,
            object: 1,
          })
        )
        .then((r: Result) => ({ term, ...r })),
    150
  );

  complete<LightUserOnline>({
    input: opts.input,
    fetch: t => debounced(t).then(({ term, result }) => (t == term ? result : Promise.reject('Debounced ' + t))),
    render(o: LightUserOnline) {
      const tag = opts.tag || 'a';
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
        '"></i>' +
        (o.title
          ? '<span class="utitle"' + (o.title == 'BOT' ? ' data-bot="data-bot" ' : '') + '>' + o.title + '</span>&nbsp;'
          : '') +
        o.name +
        '</' +
        tag +
        '>'
      );
    },
    populate: opts.populate || (r => r.name),
    onSelect: opts.onSelect,
    regex: /^[a-z][\w-]{2,29}$/i,
  });
}
