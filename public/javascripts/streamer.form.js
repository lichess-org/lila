$(function() {
  $('.streamer_picture form.upload input[type=file]').change(function() {
    $('.picture_wrap').html(lidraughts.spinnerHtml);
    $(this).parents('form').submit();
  });
});
