import debounce from "common/debounce";
import * as xhr from 'common/xhr';
import { notNull } from 'common';
import spinnerHtml from './component/spinner';
import Tagify from '@yaireo/tagify'

lichess.load.then(() => {

  var $editor = $('.coach-edit');

  var todo = (function() {

    var $overview = $editor.find('.overview');
    var $el = $overview.find('.todo');
    var $listed = $editor.find('#form3-listed');

    var must = [{
      html: '<a href="/account/profile">Complete your lichess profile</a>',
      check() {
        return $el.data('profile');
      }
    }, {
      html: 'Upload a profile picture',
      check() {
        return $editor.find('img.picture').length;
      }
    }, {
      html: 'Fill in basic information',
      check() {
        for (let name of ['profile.headline', 'languages']) {
          if (!$editor.find('[name="' + name + '"]').val()) return false;
        }
        return true;
      }
    }, {
      html: 'Fill at least 3 description texts',
      check() {
        return $editor.find('.panel.texts textarea').filter(function(this: HTMLTextAreaElement) {
          return !!$(this).val();
        }).length >= 3;
      }
    }];

    return function() {
      const points: Cash[] = must.filter(o => !o.check()).map(o => $('<li>').html(o.html));
      const $ul = $el.find('ul').empty();
      points.forEach(p => $ul.append(p));
      var fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $listed.prop('checked', false);
      $listed.prop('disabled', fail);
    };
  })();

  $editor.find('.tabs > div').on('click', function(this: HTMLElement) {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + $(this).data('tab')).addClass('active');
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
  $editor.find('input, textarea, select')
    .on("input paste change keyup", function() {
      $editor.find('div.status').removeClass('saved');
      submit();
    });

  if ($editor.find('.reviews .review').length)
    $editor.find('.tabs div[data-tab=reviews]').trigger('click');

  const $reviews = $editor.find('.reviews');
  $reviews.find('.actions a').on('click', function(this: HTMLAnchorElement) {
    const $review = $(this).parents('.review');
    xhr.text(
      $review.data('action') + '?v=' + $(this).data('value'),
      { method: 'post' }
    );
    $review.hide();
    $editor.find('.tabs div[data-tab=reviews]').data('count', ($reviews.find('.review').length - 1));
    return false;
  });

  $('.coach_picture form.upload input[type=file]').on('change', function(this: HTMLInputElement) {
    $('.picture_wrap').html(spinnerHtml);
    ($(this).parents('form')[0] as HTMLFormElement).submit();
  });

  const langInput = document.getElementById('form3-languages')!;
  const tagify = new Tagify(langInput, {
    maxTags: 10,
    whitelist: JSON.parse(langInput.getAttribute('data-all') || ''),
    enforceWhitelist: true,
    dropdown: {
      enabled: 1
    }
  });
  tagify.addTags(
    langInput.getAttribute('data-value')?.split(',').map(code =>
      tagify.settings.whitelist.find(l => l.code == code)
    ).filter(notNull)
  );

  todo();
});
