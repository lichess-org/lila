$(function() {
  lichess.refreshCoachForm = function() {
    $('form.coach-refresh').submit(function() {
      $.modal($(this).find('.crunshing'));
      $.post($(this).attr('action'), function() {
        location.reload();
      });
      return false;
    });
  };
  lichess.refreshCoachForm();
});
