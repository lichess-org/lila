$(function() {
  $(".bar-rating").barrating();
  $('.review-form .toggle').click(function() {
    $(this).remove();
    $('.review-form form').slideDown(500);
  });
});
