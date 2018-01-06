$(function() {
  $('.streamer_picture form.upload input[type=file]').change(function() {
    $('.picture_wrap').html(lichess.spinnerHtml);
    $(this).parents('form').submit();
  });
  // hackfix nested forms
  var $form = $('form.material');
  var $status = $form.find('div.status');
  if ($status.find('button').length) {
    $form.siblings().not('.top').appendTo($('form.material'));
  }
  $form.before($status.html(
    $('<form method="post" action="/streamer/approval/request">').html($status.html())
  ));
});
