$(function() {
  $('#inquiry .notes').on('mouseenter', function() {
    $(this).find('textarea')[0].focus();
  });
  $('#inquiry .costello').click(function() {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });

  var nextStore = lichess.storage.makeBoolean('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry .switcher input').attr('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry .switcher input').on('change', function() {
    nextStore.set(this.checked);
    $('#inquiry input.auto-next').val(this.checked ? '1' : '0');
  });
});
