import { loadCssPath } from "./component/assets";
import debounce from 'debounce-promise';
import * as xhr from 'common/xhr';
import complete from 'common/complete';
/* import spinnerHtml from "./spinner"; */

interface Opts {
  input: HTMLInputElement,
  tag?: 'a' | 'span';
  minLength?: number;
  onSelect?: (value: string | { id: string; name: string }) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
}

interface Result extends LightUser {
  online: boolean;
}

export default function(opts: Opts) {
  const cache = new Map<string, Result[]>();
  const sendXhr: (term: string) => Promise<Result[]> = debounce((term: string) =>
    xhr.json(
      xhr.url('/player/autocomplete', {
        term,
        friend: opts.friend ? 1 : 0,
        tour: opts.tour,
        swiss: opts.swiss,
        object: 1
      })
    ).then(r => {
      const res = r.result;
      cache.set(term, res);
      return res;
    }), 150);
  loadCssPath('complete');
  complete<Result>({
    input: opts.input,
    fetch: sendXhr,
    render(o: Result) {
      const tag = opts.tag || 'a';
      return '<' + tag + ' class="ulpt user-link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' +
        '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="utitle">' + o.title + '</span>&nbsp;' : '') + o.name +
        '</' + tag + '>';
    }
  });
  /* if (opts.focus) input.focus(); */

  /*   const ac = new autoComplete({ */
  /*     selector: () => input, */
  /*     threshold: opts.minLength || 3, */
  /*     data: { */
  /*       src: () => { */
  /*         const term = input.value.trim(); */
  /*         console.log(term); */
  /*         if (!term.match(/^[a-z0-9][\w-]{2,29}$/i)) return Promise.resolve([]); */
  /*         else if (cache.has(term)) return new Promise(res => setTimeout(() => res(cache.get(term)), 50)); */
  /*         else if ( */
  /*           term.length > 3 && Array.from({ */
  /*             length: term.length - 3 */
  /*           }, (_, i) => -i - 1).map(i => term.slice(0, i)).some(sub => */
  /*             cache.has(sub) && !cache.get(sub)!.length */
  /*           ) */
  /*         ) return Promise.resolve([]); */
  /*         else return sendXhr(term).then(res => { console.log(res, 'return data'); return res; }); */
  /*       }, */
  /*       cache: false */
  /*     } */
  /*   }); */
  /*   if (opts.focus) input.focus(); */
  /*   console.log(ac); */
  /* $input.typeahead({ */
  /*   minLength: opts.minLength || 3, */
  /* }, { */
  /*   hint: true, */
  /*   highlight: false, */
  /*   limit: 10, */
  /*   displayKey: 'name', */
  /*   templates: { */
  /*     empty: '<div class="empty">No player found</div>', */
  /*     pending: spinnerHtml, */
  /*     suggestion(o) { */
  /*       const tag = opts.tag || 'a'; */
  /*       return '<' + tag + ' class="ulpt user-link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' + */
  /*         '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="utitle">' + o.title + '</span>&nbsp;' : '') + o.name + */
  /*         '</' + tag + '>'; */
  /*     } */
  /*   } */
  /* }).on('typeahead:render', () => window.lichess.pubsub.emit('content_loaded')); */
  /* if (opts.focus) $input[0]!.focus(); */
  /* if (opts.onSelect) $input */
  /*   .on('typeahead:select', (_, sel) => opts.onSelect!(sel)) */
  /*   .on('keypress', function(this: HTMLInputElement, e) { */
  /*     if (e.which == 10 || e.which == 13) opts.onSelect!(this.value); */
  /*   }); */
}
