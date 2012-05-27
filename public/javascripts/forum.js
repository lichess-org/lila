$(function() {
    $('.checkmateCaptcha').each(function() {
      var $captcha = $(this);
      var $input = $captcha.find('input');
      var i1, i2;
      $captcha.find('div.lmcs').click(function() {
        var key = $(this).data('key');
        i1 = $input.val();
        i2 = i1.length > 3 ? key : i1 + " " + key;
        $input.val(i2);
      });
    });
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
