$(function() {

  var $form = $("form.search");
  var $result = $(".search_result");

  function realtimeResults() {
    $result.load(
      $form.attr("action") + "?" + $form.serialize() + " .search_result",
      function() {
        $('body').trigger('lichess.content_loaded');
        $result.find('.search_infinitescroll:has(.pager a)').each(function() {
          var $next = $(this).find(".pager a:last")
          $next.attr("href", $next.attr("href") + "&" + $form.serialize());
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
      });
  }

  $form.find("select").change(realtimeResults);
  $form.find("input").change(realtimeResults);

  $form.find(".opponent select").change(function() {
    $form.find(".aiLevel").toggle($(this).val() == 1);
  }).trigger("change");
});
