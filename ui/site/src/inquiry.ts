import * as xhr from 'common/xhr';

import { expandMentions } from 'common/richText';

lichess.load.then(() => {
  const noteStore = lichess.storage.make('inquiry-note');
  const usernameNoteStore = lichess.storage.make('inquiry-note-user');
  const username = $('#inquiry .meat > .user-link').text().split(' ')[0];
  if (username != usernameNoteStore.get()) noteStore.remove();
  usernameNoteStore.set(username);
  const noteTextArea = $('#inquiry .notes').find('textarea')[0] as HTMLTextAreaElement;

  $('#inquiry .notes').on('mouseenter', () => {
    noteTextArea.focus();
    noteTextArea.value = noteStore.get() || '';
  });

  const loadNotes = () => {
    const $notes = $('#inquiry .notes');
    $notes.on('input', () => setTimeout(() => noteStore.set(noteTextArea.value), 50));
    $notes.find('form').on('submit', function (this: HTMLFormElement) {
      xhr
        .formToXhr(this)
        .then(html => $notes.replaceWith(html))
        .then(noteStore.remove)
        .then(() => loadNotes())
        .catch(() => alert('Invalid note, is it too short or too long?'));
      return false;
    });
  };
  loadNotes();

  $('#inquiry .costello').on('click', () => {
    $('#inquiry').toggleClass('hidden');
    $('body').toggleClass('no-inquiry');
  });

  const nextStore = lichess.storage.makeBoolean('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry .switcher input').prop('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry .switcher input').on('change', function (this: HTMLInputElement) {
    nextStore.set(this.checked);
    $('#inquiry input.auto-next').val(this.checked ? '1' : '0');
  });

  window.Mousetrap.bind('d', () => $('#inquiry .actions.close form.process button[type="submit"]').trigger('click'));

  $('#inquiry .atom p').each(function (this: HTMLParagraphElement) {
    $(this).html(
      expandMentions(
        $(this)
          .html()
          .replace(/(?:https:\/\/)?lichess\.org\/([\w\/:(&;)?=@\.]+)/gi, '<a href="/$1">lichess.org/$1</a>')
      )
    );
  });

  $('#communication').on('click', '.line.author, .post.author', function (this: HTMLElement) {
    // Need to take username from the communcation page so that when being in inquiry for user A and checking communication of user B
    // the notes cannot be mistakenly attributed to user A.
    const username = $('#communication').find('.title').text().split(' ')[0];
    const message = $(this).find('.message').text();
    const storedNote = noteStore.get();
    noteStore.set((storedNote ? storedNote + '\n' : '') + `${username}: "${message}"`);
    const notes = $('#inquiry .notes span').addClass('flash');
    setTimeout(() => notes.removeClass('flash'), 100);
  });
});
