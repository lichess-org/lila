var tablesort = require('tablesort');

$(function () {
  $('table.sortable').each(function () {
    tablesort(this, {
      descending: true,
    });
  });
  $('.name-regen').click(function () {
    $.get($(this).attr('href'), name => $('#form3-create-username').val(name));
    return false;
  });

  $('#form3-teachers').each(function () {
    const textarea = this;

    function currentUserIds() {
      return textarea.value.split('\n').slice(0, -1);
    }

    lishogi.loadScript('vendor/textcomplete.min.js').then(function () {
      const textcomplete = new Textcomplete(new Textcomplete.editors.Textarea(textarea), {
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
          search: function (term, callback) {
            if (term.length < 2) callback([]);
            else
              $.ajax({
                url: '/api/player/autocomplete?object=1&teacher=1',
                data: {
                  term: term,
                },
                success: function (res) {
                  const current = currentUserIds();
                  callback(res.result.filter(t => !current.includes(t.id)));
                },
                error: function () {
                  callback([]);
                },
                cache: true,
              });
          },
          template: function (o, i) {
            return (
              '<span class="ulpt user-link' +
              (o.online ? ' online' : '') +
              '" href="/@/' +
              o.name +
              '">' +
              '<i class="line' +
              (o.patron ? ' patron' : '') +
              '"></i>' +
              (o.title ? '<span class="title">' + o.title + '</span>&nbsp;' : '') +
              o.name +
              '</span>'
            );
          },
          replace: function (o) {
            return '$1' + o.name + '\n';
          },
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
    }
  );
});
