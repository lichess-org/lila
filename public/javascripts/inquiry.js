window.requestIdleCallback(function() {
  $(function() {
    $('#inquiry .notes').on('mouseenter', function() {
      $(this).find('textarea')[0].focus();
    });
    $('#inquiry .costello').click(function() {
      $('#inquiry').toggleClass('hidden');
      $('body').toggleClass('no-inquiry');
    });

    var nextStore = lichess.storage.makeBoolean('inquiry-auto-next');
    var next = function() {
      return nextStore.get();
    };

    if (!next()) {
      $('#inquiry .switcher input').attr('checked', false);
      $('#inquiry input.auto-next').val('0');
    }

    $('#inquiry .switcher input').on('change', function() {
      nextStore.set(next());
      $('#inquiry input.auto-next').val(next() ? '1' : '0');
    });
  });
});
