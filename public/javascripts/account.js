$(function () {
  $(".security table form").submit(function () {
    $.post($(this).attr("action"));
    $(this)
      .parent()
      .parent()
      .fadeOut(300, function () {
        $(this).remove();
      });
    return false;
  });

  $("form.autosubmit").each(function () {
    var $form = $(this);
    $form.find("input").change(function () {
      const cfg = lishogi.formAjax($form);
      cfg.success = function () {
        $form.find(".saved").fadeIn();
        lishogi.storage.fire("reload-round-tabs");
      };
      $.ajax(cfg);
    });
  });
});
