$(function() {
  $("#lichess_forum a.delete").unbind("click").click(function() {
    if (confirm("Delete?")) {
      var $this = $(this)
      $.post($this.attr("href"), function(d) {
        $this.closest(".post").slideUp(500);
      });
    }
    return false;
  });
});
