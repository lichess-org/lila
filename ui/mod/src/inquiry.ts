import * as xhr from 'common/xhr';

import { expandMentions } from 'common/richText';

lichess.load.then(() => {
  const noteStore = lichess.storage.make('inquiry-note');
  const usernameNoteStore = lichess.storage.make('inquiry-note-user');
  const username = $('#inquiry .meat > .user-link').text().split(' ')[0];
  if (username != usernameNoteStore.get()) noteStore.remove();
  usernameNoteStore.set(username);
  const noteTextArea = $('#inquiry .notes').find('textarea')[0] as HTMLTextAreaElement;
  const syncNoteValue = () => (noteTextArea.value = noteStore.get() || '');
  let hasSeenNonEmptyNoteWarning = false;

  $('#inquiry .notes').on('mouseenter', () => {
    syncNoteValue();
    noteTextArea.focus();
  });

  const loadNotes = () => {
    const $notes = $('#inquiry .notes');
    $notes.on('input', () => setTimeout(() => noteStore.set(noteTextArea.value), 50));
    $notes.find('form button[type=submit]').on('click', function (this: HTMLButtonElement) {
      $(this)
        .parents('form')
        .each((_, form: HTMLFormElement) =>
          xhr
            .formToXhr(form, this)
            .then(html => $notes.replaceWith(html))
            .then(noteStore.remove)
            .then(() => loadNotes())
            .catch(() => alert('Invalid note, is it too short or too long?'))
        );
      return false;
    });
  };
  loadNotes();
  const flashNotes = (warning = false) => {
    const flashClass = warning ? 'note-flash warning' : 'note-flash';
    const notes = $('#inquiry .notes > span').addClass(flashClass);
    setTimeout(() => notes.removeClass(flashClass), 100);
  };

  $('#inquiry .costello').on('click', () => {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });

  const nextStore = lichess.storage.boolean('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry .switcher input').prop('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry .switcher input').on('change', function (this: HTMLInputElement) {
    nextStore.set(this.checked);
    $('#inquiry input.auto-next').val(this.checked ? '1' : '0');
  });
  $('#inquiry .actions.close').on('click', function (this: HTMLInputElement) {
    if (noteStore.get() && !hasSeenNonEmptyNoteWarning) {
      event!.preventDefault();
      const readTime = 1000;
      setTimeout(() => (hasSeenNonEmptyNoteWarning = true), readTime);
      flashNotes(true);
      syncNoteValue();
      const $noteDiv = $($('#inquiry .notes').find('div')[0]);
      $noteDiv.css('display', 'block');
      setTimeout(() => $noteDiv.css('display', ''), readTime);
    }
  });

  lichess.mousetrap.bind('d', () =>
    $('#inquiry .actions.close form.process button[type="submit"]').trigger('click')
  );

  $('#inquiry .atom p').each(function (this: HTMLParagraphElement) {
    $(this).html(
      expandMentions(
        $(this)
          .html()
          .replace(
            /(?:https:\/\/)?lichess\.org\/((?:[\w\/:(&;)=@-]|[?.]\w)+)/gi,
            '<a href="/$1">lichess.org/$1</a>'
          )
      )
    );
  });

  $('#communication').on('click', '.line.author, .post.author', function (this: HTMLElement) {
    // Need to take username from the communication page so that when being in inquiry for user A and checking communication of user B
    // the notes cannot be mistakenly attributed to user A.
    const username = $('#communication').find('.title').text().split(' ')[0];
    const message = $(this).find('.message').text();
    const storedNote = noteStore.get();
    noteStore.set((storedNote ? storedNote + '\n' : '') + `${username}: "${message}"`);
    flashNotes();
  });
});
