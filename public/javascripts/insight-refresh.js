$(function () {
  lishogi.refreshInsightForm = function () {
    $("form.insight-refresh:not(.armed)")
      .addClass("armed")
      .submit(function () {
        $.modal($(this).find(".crunching"));
        $.post($(this).attr("action"), function () {
          lishogi.reload();
        });
        return false;
      });
  };
  lishogi.refreshInsightForm();
});
