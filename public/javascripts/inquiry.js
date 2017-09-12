$(function() {
  $('#inquiry .notes').on('mouseenter', function() {
    $(this).find('textarea')[0].focus();
  });
  $('#inquiry .costello').click(function() {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });
});
