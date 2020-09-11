import { loadCssPath } from "./component/assets";
import debounce from 'debounce-promise';
import * as xhr from 'common/xhr';
import complete from 'common/complete';

interface Result extends LightUser {
  online: boolean;
}

interface Opts {
  input: HTMLInputElement,
  tag?: 'a' | 'span';
  minLength?: number;
  select?: (result: Result) => string;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
}

export default function(opts: Opts) {

  loadCssPath('complete');

  complete<Result>({
    input: opts.input,
    fetch: debounce(
      (term: string) =>
        xhr.json(
          xhr.url('/player/autocomplete', {
            term,
            friend: opts.friend ? 1 : 0,
            tour: opts.tour,
            swiss: opts.swiss,
            object: 1
          })
        ).then(r => r.result),
      150),
    render(o: Result) {
      const tag = opts.tag || 'a';
      return '<' + tag + ' class="complete-result ulpt user-link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' +
        '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="utitle">' + o.title + '</span>&nbsp;' : '') + o.name +
        '</' + tag + '>';
    },
    select: opts.select || (r => r.name),
    regex: /^[a-z0-9][\w-]{2,29}$/i
  });
}
