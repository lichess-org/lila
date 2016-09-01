$(function() {
  var $editor = $('.coach_edit');
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
      url: $review.data('action') + '?v=' + $(this).data('value')
    });
    $review.slideUp(300);
    $editor.find('.tabs div[data-tab=reviews]').attr('data-count', $reviews.find('.review').length - 1);
    return false;
  });
});
