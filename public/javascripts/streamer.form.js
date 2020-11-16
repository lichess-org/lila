$(function () {
  $(".streamer_picture form.upload input[type=file]").change(function () {
    $(".picture_wrap").html(lishogi.spinnerHtml);
    $(this).parents("form").submit();
  });
});
