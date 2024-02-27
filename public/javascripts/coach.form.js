$(function () {
  var $editor = $('.coach-edit');

  var todo = (function () {
    var $overview = $editor.find('.overview');
    var $el = $overview.find('.todo');
    var $listed = $editor.find('#form3-listed');

    var must = [
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
      var points = [];
      for (let o of must) if (!o.check()) points.push($('<li>').html(o.html));
      $el.find('ul').html(points);
      var fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $listed.prop('checked', false);
      $listed.attr('disabled', fail);
    };
  })();

  $editor.find('.tabs > div').click(function () {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + $(this).data('tab')).addClass('active');
    $editor.find('div.status').removeClass('saved');
  });
  var submit = lishogi.debounce(function () {
    const $asyncForm = $editor.find('form.async');
    if ($asyncForm.length)
      $asyncForm.ajaxSubmit({
        success: function () {
          $editor.find('div.status').addClass('saved');
          todo();
        },
      });
  }, 1000);
  $editor.find('input, textarea, select').on('input paste change keyup', function () {
    $editor.find('div.status').removeClass('saved');
    submit();
  });

  $('.coach_picture form.upload input[type=file]').change(function () {
    $('.picture_wrap').html(lishogi.spinnerHtml);
    $(this).parents('form').submit();
  });

  const langInput = document.getElementById('form3-languages');
  const tagify = new Tagify(langInput, {
    delimiters: null,
    maxTags: 10,
    whitelist: JSON.parse(langInput.getAttribute('data-all')),
    templates: {
      tag: function (v, tagData) {
        return `<tag title='${v}' contenteditable='false' spellcheck="false" class='tagify__tag ${
          tagData.class ? tagData.class : ''
        }' ${this.getAttributes(tagData)}>
                <x title='remove tag' class='tagify__tag__removeBtn'></x>
                <div>
                    <span class='tagify__tag-text'>${v}</span>
                </div>
            </tag>`;
      },
      dropdownItem: function (tagData) {
        return `<div class='tagify__dropdown__item ${tagData.class ? tagData.class : ''}'>
                  <span>${tagData.value}</span>
              </div>`;
      },
    },
    enforceWhitelist: true,
    dropdown: {
      enabled: 1,
    },
  });
  tagify.addTags(
    langInput
      .getAttribute('data-value')
      .split(',')
      .map(code => tagify.settings.whitelist.find(l => l.code == code))
      .filter(x => x)
  );

  todo();
});
