import debounce from 'common/debounce';
import * as xhr from 'common/xhr';
import { isSafari } from 'common/device';
import { notNull } from 'common';
import Tagify from '@yaireo/tagify';
import { wireCropDialog } from './exports/crop';

if (isSafari()) wireCropDialog(); // preload

site.load.then(() => {
  const $editor = $('.coach-edit');

  const todo = (function () {
    const $overview = $editor.find('.overview');
    const $el = $overview.find('.todo');
    const $listed = $editor.find('#form3-listed');

    const must = [
      {
        html: '<a href="/account/profile">Complete your lichess profile</a>',
        check() {
          return $el.data('profile');
        },
      },
      {
        html: 'Upload a profile picture',
        check() {
          return $editor.find('img.picture').length;
        },
      },
      {
        html: 'Fill in basic information',
        check() {
          for (const name of ['profile.headline', 'languages']) {
            if (!$editor.find('[name="' + name + '"]').val()) return false;
          }
          return true;
        },
      },
      {
        html: 'Fill at least 3 description texts',
        check() {
          return (
            $editor.find('.panel.texts textarea').filter(function (this: HTMLTextAreaElement) {
              return !!$(this).val();
            }).length >= 3
          );
        },
      },
    ];

    return function () {
      const points: Cash[] = must.filter(o => !o.check()).map(o => $('<li>').html(o.html));
      const $ul = $el.find('ul').empty();
      points.forEach(p => $ul.append(p));
      const fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $listed.prop('checked', false);
      $listed.prop('disabled', fail);
    };
  })();

  {
    // languages
    const langInput = document.getElementById('form3-languages') as HTMLInputElement;
    const whitelistJson = langInput.getAttribute('data-all');
    const whitelist = whitelistJson ? (JSON.parse(whitelistJson) as Tagify.TagData[]) : undefined;
    const tagify = new Tagify(langInput, {
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
        .filter(notNull) as Tagify.TagData[],
    );
  }

  $editor.find('.tabs > div').on('click', function (this: HTMLElement) {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + this.dataset.tab).addClass('active');
    $editor.find('div.status').removeClass('saved');
  });
  const submit = debounce(() => {
    const form = document.querySelector('form.async') as HTMLFormElement;
    if (!form) return;
    xhr.formToXhr(form).then(() => {
      $editor.find('div.status').addClass('saved');
      todo();
    });
  }, 1000);

  $('.coach_picture form.upload input[type=file]').on('change', function (this: HTMLInputElement) {
    $('.picture_wrap').html(site.spinnerHtml);
    ($(this).parents('form')[0] as HTMLFormElement).submit();
  });

  setTimeout(() => {
    $editor.find('input, textarea, select').on('input paste change keyup', function () {
      $editor.find('div.status').removeClass('saved');
      submit();
    });
    todo();
  }, 1000);

  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/coach', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
});
