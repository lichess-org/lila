$(function() {
  lidraughts.refreshInsightForm = function() {
    $('form.insight-refresh:not(.armed)').addClass('armed').submit(function() {
      $.modal($(this).find('.crunching'));
      $.post($(this).attr('action'), function() {
        lidraughts.reload();
      });
      return false;
    });
  };
  lidraughts.refreshInsightForm();
});
