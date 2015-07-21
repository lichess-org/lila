$(function() {
  $('#coach_side form.refresh').submit(function() {
    $.modal($('.refreshing'));
    $.post($(this).attr('action'), function() {
      location.reload();
    });
    return false;
  });
});
