$(function() {
  lichess.refreshInsightForm = function() {
    $('form.insight-refresh').submit(function() {
      $.modal($(this).find('.crunching'));
      $.post($(this).attr('action'), function() {
        lichess.reload();
      });
      return false;
    });
  };
  lichess.refreshInsightForm();
});
