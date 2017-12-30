$(function() {

  var $editor = $('.streamer_edit');

  var submit = lichess.fp.debounce(function() {
    $editor.find('form.form').ajaxSubmit({
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

  $('.streamer_picture form.upload input[type=file]').change(function() {
    $('.picture_wrap').html(lichess.spinnerHtml);
    $(this).parents('form').submit();
  });
});
