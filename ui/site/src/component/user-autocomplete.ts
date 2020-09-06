import { loadCssPath, loadScript } from "./assets";
import debounce from 'common/debounce';
import * as xhr from 'common/xhr';
import spinnerHtml from "./spinner";

export default function($input: JQuery, opts: UserAutocompleteOpts = {}): Promise<void> {
  const cache = {};
  loadCssPath('autocomplete');
  const sendXhr = debounce((query, runAsync) =>
    xhr.json(
      xhr.url('/player/autocomplete', {
        term: query,
        friend: opts.friend ? 1 : 0,
        tour: opts.tour,
        swiss: opts.swiss,
        object: 1
      })
    ).then(res => {
      res = res.result;
      // hack to fix typeahead limit bug
      if (res.length === 10) res.push(null);
      cache[query] = res;
      runAsync(res)
    }), 150);
  return loadScript('javascripts/vendor/typeahead.jquery.min.js').then(() => {
    $input.typeahead({
      minLength: opts.minLength || 3,
    }, {
      hint: true,
      highlight: false,
      source(query, _, runAsync) {
        query = query.trim();
        if (!query.match(/^[a-z0-9][\w-]{2,29}$/i)) return;
        else if (cache[query]) setTimeout(() => runAsync(cache[query]), 50);
        else if (
          query.length > 3 && Array.from({
            length: query.length - 3
          }, (_, i) => -i - 1).map(i => query.slice(0, i)).some(sub =>
            cache[sub] && !cache[sub].length
          )
        ) return;
        else sendXhr(query, runAsync);
      },
      limit: 10,
      displayKey: 'name',
      templates: {
        empty: '<div class="empty">No player found</div>',
        pending: spinnerHtml,
        suggestion(o) {
          const tag = opts.tag || 'a';
          return '<' + tag + ' class="ulpt user-link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' +
            '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="utitle">' + o.title + '</span>&nbsp;' : '') + o.name +
            '</' + tag + '>';
        }
      }
    }).on('typeahead:render', () => window.lichess.pubsub.emit('content_loaded'));
    if (opts.focus) $input.focus();
    if (opts.onSelect) $input
      .on('typeahead:select', (_, sel) => opts.onSelect!(sel))
      .on('keypress', function(this: HTMLElement, e) {
        if (e.which == 10 || e.which == 13) opts.onSelect!($(this).val());
      });
  });
};
