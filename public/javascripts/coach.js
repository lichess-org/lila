$(function() {
  $(".bar-rating").barrating({
    theme: 'fontawesome-stars'
  });
  $('.review-form .toggle').click(function() {
    $(this).remove();
    $('.review-form form').slideDown(500);
  });
});
