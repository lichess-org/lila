import { sortTable, extendTablesortNumber } from 'lib/tablesort';
import * as xhr from 'lib/xhr';
import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

import type { UserCompleteResult } from 'lib/view/userComplete';

site.load.then(() => {
  $('table.sortable').each(function (this: HTMLElement) {
    sortTable(this, {
      descending: false,
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
    new Textcomplete(new TextareaEditor(textarea), [
      {
        id: 'teacher',
        match: /(^|\s)(.+)$/,
        index: 2,
        search(term: string, callback: (res: any[]) => void) {
          if (term.length < 3) callback([]);
          else
            xhr.json(xhr.url('/api/player/autocomplete', { object: 1, teacher: 1, term })).then(
              (res: UserCompleteResult) => {
                const current = currentUserIds();
                callback(res.result.filter(t => !current.includes(t.id)));
              },
              _ => callback([]),
            );
        },
        template: (o: LightUserOnline) =>
          '<span class="ulpt user-link' +
          (o.online ? ' online' : '') +
          '" data-href="/@/' +
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
  });

  extendTablesortNumber();
});
