import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

import { sortTable, extendTablesortNumber } from 'lib/tablesort';
import type { UserCompleteResult } from 'lib/view/userComplete';
import * as xhr from 'lib/xhr';

site.load.then(() => {
  $('table.sortable').each(function (this: HTMLTableElement) {
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
    new Textcomplete(new TextareaEditor(textarea), [
      {
        id: 'teacher',
        match: /(^|\s)(.+)$/,
        index: 2,
        search(term, searchCallback) {
          if (term.length < 3) searchCallback([]);
          else
            xhr.json(xhr.url('/api/player/autocomplete', { object: 1, teacher: 1, term })).then(
              (res: UserCompleteResult) => {
                const current = currentUserIds(textarea.value);
                searchCallback(res.result.filter(t => !current.includes(t.id)));
              },
              _ => searchCallback([]),
            );
        },
        template: ({ online, name, patron, title }: LightUserOnline) =>
          `<span class="ulpt user-link${online ? ' online' : ''}" data-href="/@/${name}">` +
          `<icon class="line${patron ? ' patron' : ''}"></icon>` +
          `${title ? '<span class="utitle">' + title + '</span>&nbsp;' : ''}${name}</span>`,
        replace: ({ name }: LightUserOnline) => `$1${name}\n`,
      },
    ]);
  });

  extendTablesortNumber();
});

function currentUserIds(value: string) {
  return value.split('\n').slice(0, -1);
}
