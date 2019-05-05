$(function() {

  var $editor = $('.coach-edit');

  var todo = (function() {

    var $overview = $editor.find('.overview');
    var $el = $overview.find('.todo');
    var $checkbox = $editor.find('#form3-listed');

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
        ['profile.headline', 'profile.languages'].forEach(function(name) {
          if (!$editor.find('[name="' + name + '"]').val()) return false;
        });
        return true;
      }
    }, {
      html: 'Fill at least 3 description texts',
      check: function() {
        return $editor.find('.panel.texts textarea').filter(function() {
          return !!$(this).val();
        }).length >= 3;
      }
    }];

    return function() {
      var points = [];
      must.forEach(function(o) {
        if (!o.check()) points.push($('<li>').html(o.html));
      });
      $el.find('ul').html(points);
      var fail = !!points.length;
      $overview.toggleClass('with-todo', fail);
      if (fail) $checkbox.prop('checked', false);
      $checkbox.attr('disabled', fail);
    };
  })();
  todo();

  $editor.find('.tabs > div').click(function() {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + $(this).data('tab')).addClass('active');
    $editor.find('div.status').removeClass('saved');
  });
  var submit = lichess.debounce(function() {
    $editor.find('form.async').ajaxSubmit({
      success: function() {
        $editor.find('div.status').addClass('saved');
        todo();
      }
    });
  }, 1000);
  $editor.find('input, textarea, select')
    .on("input paste change keyup", function() {
      $editor.find('div.status').removeClass('saved');
      submit();
    });

  if ($editor.find('.reviews .review').length)
    $editor.find('.tabs div[data-tab=reviews]').click();

  $reviews = $editor.find('.reviews');
  $reviews.find('.actions a').click(function() {
    var $review = $(this).parents('.review');
    $.ajax({
      method: 'post',
      url: $review.data('action') + '?v=' + $(this).data('value')
    });
    $review.hide();
    $editor.find('.tabs div[data-tab=reviews]').attr('data-count', $reviews.find('.review').length - 1);
    return false;
  });

  $('.coach_picture form.upload input[type=file]').change(function() {
    $('.picture_wrap').html(lichess.spinnerHtml);
    $(this).parents('form').submit();
  });
});
