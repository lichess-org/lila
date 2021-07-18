import tablesort from 'tablesort';

import { loadScript } from './component/assets';
import extendTablesortNumber from './component/tablesort-number';
import * as xhr from 'common/xhr';

import type { Result as UserCompleteResult } from './userComplete';

lichess.load.then(() => {
  $('table.sortable').each(function (this: HTMLElement) {
    tablesort(this, {
      descending: true,
    });
  });
  $('.name-regen').on('click', function (this: HTMLAnchorElement) {
    xhr.text(this.href).then(name => $('#form3-create-username').val(name));
    return false;
  });

  $('#form3-teachers').each(function (this: HTMLTextAreaElement) {
    const textarea = this;

    function currentUserIds() {
      return textarea.value.split('\n').slice(0, -1);
    }

    loadScript('vendor/textcomplete.min.js').then(() => {
      const textcomplete = new window.Textcomplete(new window.Textcomplete.editors.Textarea(textarea), {
        dropdown: {
          maxCount: 10,
          placement: 'bottom',
        },
      });

      textcomplete.register([
        {
          id: 'teacher',
          match: /(^|\s)(.+)$/,
          index: 2,
          search(term: string, callback: (res: any[]) => void) {
            if (term.length < 2) callback([]);
            else
              xhr
                .json(
                  xhr.url('/player/autocomplete', {
                    object: 1,
                    teacher: 1,
                    term,
                  })
                )
                .then(
                  (res: UserCompleteResult) => {
                    const current = currentUserIds();
                    callback(res.result.filter(t => !current.includes(t.id)));
                  },
                  _ => callback([])
                );
          },
          template: (o: LightUserOnline) =>
            '<span class="ulpt user-link' +
            (o.online ? ' online' : '') +
            '" href="/@/' +
            o.name +
            '">' +
            '<i class="line' +
            (o.patron ? ' patron' : '') +
            '"></i>' +
            (o.title ? '<span class="utitle">' + o.title + '</span>&nbsp;' : '') +
            o.name +
            '</span>',
          replace: (o: LightUserOnline) => '$1' + o.name + '\n',
        },
      ]);

      textcomplete.on('rendered', function () {
        if (textcomplete.dropdown.items.length) {
          // Activate the first item by default.
          textcomplete.dropdown.items[0].activate();
        }
      });
    });
  });

  extendTablesortNumber();
});
