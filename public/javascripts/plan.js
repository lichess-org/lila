$(function () {
  $table = $('.plan table.all');
  $change = $table.find('.change');
  $change.find('a').on('click', function () {
    var f = $(this).data('form');
    $change.find('form:not(.' + f + ')').hide();
    $change.find('form.' + f).toggle();
  });
});
