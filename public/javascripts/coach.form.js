$(function() {
  var $editor = $('.coach_edit');
  $editor.find('.tabs > div').click(function() {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    $editor.find('.panel.' + $(this).data('tab')).addClass('active');
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
});
