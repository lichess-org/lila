$(function() {

  $('.security.content_box table form').submit(function() {
    $.post($(this).attr('action'));
    $(this).parent().parent().fadeOut(300, function() { $(this).remove(); });
    return false;
  });
});
