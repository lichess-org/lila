import tablesort from 'tablesort';
import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

window.lishogi.ready.then(() => {
  $('table.sortable').each(function () {
    tablesort(this, {
      descending: true,
    });
  });
  $('.name-regen').on('click', function (this: HTMLAnchorElement) {
    window.lishogi.xhr.text('GET', this.href).then(name => $('#form3-create-username').val(name));
    return false;
  });

  $('#form3-teachers').each(function () {
    const textarea = this as HTMLTextAreaElement;

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
            window.lishogi.xhr
              .json('GET', '/api/player/autocomplete', {
                url: {
                  object: true,
                  teacher: true,
                  term,
                },
              })
              .then(
                (res: any) => {
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

  function cleanNumber(i) {
    return i.replace(/[^\-?0-9.]/g, '');
  }

  function compareNumber(a, b) {
    a = parseFloat(a);
    b = parseFloat(b);

    a = isNaN(a) ? 0 : a;
    b = isNaN(b) ? 0 : b;

    return a - b;
  }

  tablesort.extend(
    'number',
    function (item) {
      return item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/); // Number
    },
    function (a, b) {
      return compareNumber(cleanNumber(b), cleanNumber(a));
    },
  );
});
