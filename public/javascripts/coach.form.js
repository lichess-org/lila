$(function() {

  var $editor = $('.coach_edit');

  var todo = (function() {

    var $overview = $editor.find('.overview');
    var $el = $overview.find('.todo');
    var $option = $editor.find('select[name=listed] option[value=true]');

    var must = [{
      name: 'Complete your lichess profile',
      check: function() {
        return $el.data('profile');
      }
    }, {
      name: 'Upload a profile picture',
      check: function() {
        return $editor.find('img.picture').length;
      }
    }, {
      name: 'Fill in basic informations',
      check: function() {
        ['profile.headline', 'profile.languages'].forEach(function(name) {
          if (!$editor.find('[name="' + name + '"]').val()) return false;
        });
        return true;
      }
    }, {
      name: 'Fill at least 3 description texts',
      check: function() {
        return $editor.find('.panel.texts textarea').filter(function() {
          return !!$(this).val();
        }).length >= 3;
      }
    }];

    return function() {
      var points = [];
      must.forEach(function(o) {
        if (!o.check()) points.push($('<li>').text(o.name));
      });
      $el.find('ul').html(points);
      var fail = !!points.length;
      $overview.toggleClass('with_todo', fail);
      if (fail) $option.parent().val('false');
      $option.attr('disabled', fail);
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
  var submit = $.fp.debounce(function() {
    $editor.find('form.form').ajaxSubmit({
      success: function() {
        $editor.find('div.status').addClass('saved');
      }
    });
  }, 1000);
  $editor.find('input, textarea, select')
    .bind("input paste change keyup", function() {
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
      url: $review.data('action') + '?v=' + $(this).data('value'),
      complete: function() {
        todo();
      }
    });
    $review.slideUp(300);
    $editor.find('.tabs div[data-tab=reviews]').attr('data-count', $reviews.find('.review').length - 1);
    return false;
  });

  $editor.find('.analytics .pageview_chart').each(function() {
    var $el = $(this);
    $.getJSON('/monitor/coach/pageview', function(data) {
      lichess.coachPageViewChart(data, $el);
    });
  });
});
