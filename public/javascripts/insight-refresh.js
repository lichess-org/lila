$(function() {
  lichess.refreshInsightForm = function() {
    $('form.insight-refresh').submit(function() {
      $.modal($(this).find('.crunching'));
      $.post($(this).attr('action'), function() {
        location.reload();
      });
      return false;
    });
  };
  lichess.refreshInsightForm();
});
