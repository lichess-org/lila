$(function() {
  $('#inquiry .notes').on('mouseenter', function() {
    $(this).find('textarea')[0].focus();
  });
  $('#inquiry .costello').click(function() {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });

  var nextStore = lichess.storage.make('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry .switcher input').attr('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry .switcher input').on('change', function() {
    if (nextStore.get()) nextStore.remove();
    else nextStore.set(1);
    $('#inquiry input.auto-next').val(nextStore.get() || '0');
  });
});
