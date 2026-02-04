import { formToXhr } from 'lib/xhr';

import { storage } from 'lib/storage';
import { alert } from 'lib/view';
import { highlightSearchTerm } from 'lib/highlight';
import { pubsub } from 'lib/pubsub';
import { autolinkAtoms } from './mod.autolink';

site.load.then(() => {
  const noteStore = storage.make('inquiry-note');
  const usernameNoteStore = storage.make('inquiry-note-user');
  const username = $('#inquiry').data('username');
  if (username !== usernameNoteStore.get()) noteStore.remove();
  usernameNoteStore.set(username);
  const noteTextArea = $('#inquiry .notes').find('textarea')[0] as HTMLTextAreaElement;
  const syncNoteValue = () => (noteTextArea.value = noteStore.get() || '');
  let hasSeenNonEmptyNoteWarning = false;
  $('#inquiry .notes').on('mouseenter', () => {
    syncNoteValue();
    noteTextArea.focus();
  });

  function addToNote(str: string) {
    const storedNote = noteStore.get();
    noteStore.set((storedNote ? storedNote + '\n' : '') + str);
    flashNotes();
  }

  const loadNotes = () => {
    const $notes = $('#inquiry .notes');
    $notes.on('input', () => setTimeout(() => noteStore.set(noteTextArea.value), 50));
    $notes.find('form button[value=copy-url]').on('click', event => {
      event.preventDefault();
      addToNote(location.href);
      syncNoteValue();
    });
    $notes.find('form button[type=submit]').on('click', function (this: HTMLButtonElement) {
      $(this)
        .parents('form')
        .each((_, form: HTMLFormElement) =>
          formToXhr(form, this)
            .then(html => $notes.replaceWith(html))
            .then(noteStore.remove)
            .then(() => loadNotes())
            .catch(() => alert('Invalid note, is it too short or too long?')),
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

  const nextStore = storage.boolean('inquiry-auto-next');

  if (!nextStore.get()) {
    $('#inquiry #auto-next').prop('checked', false);
    $('#inquiry input.auto-next').val('0');
  }

  $('#inquiry #auto-next').on('change', function (this: HTMLInputElement) {
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

  site.mousetrap.bind('d', () =>
    $('#inquiry .actions.close form.process button[type="submit"]').trigger('click'),
  );
  autolinkAtoms();

  $('#communication').on('click', '.line.author, .post.author', function (this: HTMLElement) {
    // Need to take username from the communication page so that when being in inquiry for user A and checking communication of user B
    // the notes cannot be mistakenly attributed to user A.
    const username = $('#communication').find('.title').text().split(' ')[0];
    const message = $(this).find('.message').text();
    addToNote(`${username}: "${message}"`);
  });
  $('#communication').on('click', '.mod-timeline__event .message', function (this: HTMLElement) {
    addToNote(`${username}: "${$(this).text()}"`);
  });

  $('.user-show, .appeal').on('click', '.mz-section--others .add-to-note', function (this: HTMLElement) {
    const userRow = $(this).parents('tr');
    addToNote(`Alt: ${[userRow.data('title') || '', `@${userRow.data('username')}`].join(' ').trim()}`);
  });

  const highlightUsername = () => highlightSearchTerm(username, '#main-wrap .user-link');
  setTimeout(highlightUsername, 300);
  pubsub.on('content-loaded', highlightUsername);
});
