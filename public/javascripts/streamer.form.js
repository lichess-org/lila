$(function() {
  $('.streamer_picture form.upload input[type=file]').on('change', function() {
    $('.picture_wrap').html(lichess.spinnerHtml);
    $(this).parents('form').trigger('submit');
  });
});
