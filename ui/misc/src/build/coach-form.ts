import { spinnerHtml } from 'common/spinner';
import { debounce } from 'common/timings';

window.lishogi.ready.then(() => {
  const $editor = $('.coach-edit');

  const todo = (function () {
    const $overview = $editor.find('.overview'),
      $el = $overview.find('.todo'),
      $listed = $editor.find('#form3-listed');

    const must = [
      {
        html: '<a href="/account/profile">Complete your lishogi profile</a>',
        check: function () {
          return $el.data('profile');
        },
      },
      {
        html: 'Upload a profile picture',
        check: function () {
          return $editor.find('img.picture').length;
        },
      },
      {
        html: 'Fill in basic information',
        check: function () {
          for (let name of ['profile.headline', 'languages']) {
            if (!$editor.find('[name="' + name + '"]').val()) return false;
          }
          return true;
        },
      },
      {
        html: 'Fill at least 3 description texts',
        check: function () {
          return (
            $editor.find('.panel.texts textarea').filter(function () {
              return !!$(this).val();
            }).length >= 3
          );
        },
      },
    ];

    return function () {
      const points: JQuery[] = [];
      for (let o of must) if (!o.check()) points.push($('<li>').html(o.html));
      $el.find('ul').empty();
      const fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $listed.prop('checked', false);
      $listed.prop('disabled', fail);
    };
  })();

  $editor.find('.tabs > div').click(function () {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + $(this).data('tab')).addClass('active');
    $editor.find('div.status').removeClass('saved');
  });
  const submit = debounce(function () {
    const form = document.querySelector('form.async') as HTMLFormElement;
    if (!form) return;
    window.lishogi.xhr.formToXhr(form).then(() => {
      $editor.find('div.status').addClass('saved');
      todo();
    });
  }, 1000);
  $editor.find('input, textarea, select').on('input paste change keyup', function () {
    $editor.find('div.status').removeClass('saved');
    submit();
  });

  $('.coach_picture form.upload input[type=file]').change(function () {
    $('.picture_wrap').html(spinnerHtml);
    $(this).parents('form').submit();
  });

  const langInput = document.getElementById('form3-languages') as HTMLInputElement;
  const whitelistJson = langInput.getAttribute('data-all');
  const whitelist = whitelistJson ? (JSON.parse(whitelistJson) as Tagify.TagData[]) : undefined;
  const tagify = new window.Tagify(langInput, {
    maxTags: 10,
    whitelist,
    enforceWhitelist: true,
    dropdown: {
      enabled: 1,
    },
  });
  tagify.addTags(
    langInput
      .getAttribute('data-value')
      ?.split(',')
      .map(code => whitelist?.find(l => l.code == code))
      .filter(v => !!v) as Tagify.TagData[],
  );
  const a: any = langInput
    .getAttribute('data-value')!
    .split(',')
    .map(code => tagify.settings.whitelist.find(l => (l as any).code == code)!)
    .filter(x => x);
  tagify.addTags(a);

  todo();
});
