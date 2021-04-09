$(function () {
  const noteStore = lichess.storage.make('inquiry-note');
  const noteTextArea = $('#inquiry .notes').find('textarea')[0];

  $('#inquiry .notes').on('mouseenter', () => {
    noteTextArea.focus();
    noteTextArea.value = noteStore.get();
  });

  $('#inquiry .notes').on('change', () => setTimeout(() => noteStore.set(noteTextArea.value), 50));

  $('#inquiry .notes').on('submit', () => noteStore.remove);

  $('#inquiry .costello').on('click', () => {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });

  const nextStore = lichess.storage.makeBoolean('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry .switcher input').prop('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry .switcher input').on('change', function () {
    nextStore.set(this.checked);
    $('#inquiry input.auto-next').val(this.checked ? '1' : '0');
  });

  Mousetrap.bind('d', () => $('#inquiry .actions.close form.process button[type="submit"]').trigger('click'));

  $('#inquiry .atom p').each(function () {
    $(this).html(
      $(this)
        .html()
        .replaceAll(/(?:https:\/\/)?lichess\.org\/([\w\/]+)/g, '<a href="/$1">lichess.org/$1</a>')
    );
  });
});
