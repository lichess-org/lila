$(function() {

  $('.search_infinitescroll:has(.pager a)').each(function() {
    var $next = $(this).find(".pager a:last")
    $next.attr("href", $next.attr("href") + "&" + $("form.search").serialize());
    $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: $next,
      itemSelector: ".search_infinitescroll .paginated_element",
      loading: {
        msgText: "",
      img: "/assets/images/hloader3.gif",
      finishedMsg: "---"
      }
    }, function() {
      $("#infscr-loading").remove();
      $('body').trigger('lichess.content_loaded');
    });
  });

  var $form = $("form.search");
  $form.find(".opponent select").change(function() {
    $form.find(".aiLevel").toggle($(this).val() == 1);
  }).trigger("change");
});
