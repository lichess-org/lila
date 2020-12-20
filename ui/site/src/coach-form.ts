import debounce from "common/debounce";
import * as xhr from 'common/xhr';
import spinnerHtml from './component/spinner';
import { notNull } from 'common';

window.lichess.load.then(() => {

  var $editor = $('.coach-edit');

  var todo = (function() {

    var $overview = $editor.find('.overview');
    var $el = $overview.find('.todo');
    var $listed = $editor.find('#form3-listed');

    var must = [{
      html: '<a href="/account/profile">Complete your lichess profile</a>',
      check: function() {
        return $el.data('profile');
      }
    }, {
      html: 'Upload a profile picture',
      check: function() {
        return $editor.find('img.picture').length;
      }
    }, {
      html: 'Fill in basic information',
      check: function() {
        for (let name of ['profile.headline', 'languages']) {
          if (!$editor.find('[name="' + name + '"]').val()) return false;
        }
        return true;
      }
    }, {
      html: 'Fill at least 3 description texts',
      check: function() {
        return $editor.find('.panel.texts textarea').filter(function(this: HTMLTextAreaElement) {
          return !!$(this).val();
        }).length >= 3;
      }
    }];

    return function() {
      const points: JQuery[] = [];
      for (let o of must) if (!o.check()) points.push($('<li>').html(o.html));
      $el.find('ul').html(points as any);
      var fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $listed.prop('checked', false);
      $listed.prop('disabled', fail);
    };
  })();

  $editor.find('.tabs > div').click(function(this: HTMLElement) {
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
    $editor.find('.tabs div[data-tab=reviews]').click();

  const $reviews = $editor.find('.reviews');
  $reviews.find('.actions a').click(function(this: HTMLAnchorElement) {
    const $review = $(this).parents('.review');
    xhr.text(
      $review.data('action') + '?v=' + $(this).data('value'),
      { method: 'post' }
    );
    $review.hide();
    $editor.find('.tabs div[data-tab=reviews]').attr('data-count', $reviews.find('.review').length - 1);
    return false;
  });

  $('.coach_picture form.upload input[type=file]').change(function(this: HTMLInputElement) {
    $('.picture_wrap').html(spinnerHtml);
    $(this).parents('form').submit();
  });

  const langInput = document.getElementById('form3-languages');
  const tagify = new window.Tagify(langInput, {
    delimiters: null,
    maxTags: 10,
    whitelist: JSON.parse(langInput?.getAttribute('data-all') || ''),
    templates: {
      tag: function(this: any, v, tagData) {
        return `<tag title='${v}' contenteditable='false' spellcheck="false" class='tagify__tag ${tagData.class ? tagData.class : ""}' ${this.getAttributes(tagData)}>
                <x title='remove tag' class='tagify__tag__removeBtn'></x>
                <div>
                    <span class='tagify__tag-text'>${v}</span>
                </div>
            </tag>`;
      },
      dropdownItem: function(tagData) {
        return `<div class='tagify__dropdown__item ${tagData.class ? tagData.class : ""}'>
                  <span>${tagData.value}</span>
              </div>`;
      }
    },
    enforceWhitelist: true,
    dropdown: {
      enabled: 1
    }
  });
  tagify.addTags(
    langInput?.getAttribute('data-value')?.split(',').map(code =>
      tagify.settings.whitelist.find(l => l.code == code)
    ).filter(notNull)
  );

  todo();
});
