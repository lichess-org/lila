import { loadCssPath, loadVendorScript } from 'common/assets';
import { spinnerHtml } from 'common/spinner';
import { pubsub } from './pubsub';
import { json } from './xhr';

export interface UserCompleteOpts {
  tag?: 'a' | 'span';
  minLength?: number;
  onSelect?: (result: any) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  team?: string;
}

export function userAutocomplete($input: JQuery, opts: UserCompleteOpts): Promise<any> {
  opts = opts || {};
  loadCssPath('misc.autocomplete');
  return loadVendorScript('typeahead', 'typeahead.jquery.min.js').then(() => {
    $input
      .typeahead(
        {
          minLength: opts.minLength || 3,
        },
        {
          hint: true,
          highlight: false,
          source: function (query: string, _: any, runAsync: (arg: any) => Promise<void>) {
            if (query.trim().match(/^[a-z0-9][\w-]{2,29}$/i))
              json(
                'GET',
                '/api/player/autocomplete',
                {
                  url: {
                    term: query,
                    friend: opts.friend ? 1 : 0,
                    tour: opts.tour,
                    object: 1,
                  },
                },
                {
                  cache: 'default',
                },
              ).then((res: any) => {
                res = res.result;
                // hack to fix typeahead limit bug
                if (res.length === 10) res.push(null);
                runAsync(res);
              });
          },
          limit: 10,
          displayKey: 'name',
          templates: {
            empty: '<div class="empty">No player found</div>',
            pending: spinnerHtml,
            suggestion: function (o: any) {
              const tag = opts.tag || 'a';
              return (
                '<' +
                tag +
                ' class="ulpt user-link' +
                (o.online ? ' online' : '') +
                '" ' +
                (tag === 'a' ? '' : 'data-') +
                'href="/@/' +
                o.name +
                '">' +
                '<i class="line' +
                (o.patron ? ' patron' : '') +
                '"></i>' +
                (o.title ? '<span class="title">' + o.title + '</span>&nbsp;' : '') +
                o.name +
                '</' +
                tag +
                '>'
              );
            },
          },
        },
      )
      .on('typeahead:render', () => pubsub.emit('content_loaded'));
    if (opts.focus) $input.focus();
    if (opts.onSelect)
      $input
        .on('typeahead:select', (_, sel) => opts.onSelect && opts.onSelect(sel))
        .on('keypress', function (e) {
          if (e.which == 10 || e.which == 13) opts.onSelect && opts.onSelect($(this).val());
        });
  });
}
