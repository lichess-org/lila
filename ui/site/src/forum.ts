import * as xhr from 'common/xhr';
import modal from 'common/modal';

lichess.load.then(() => {
  $('.forum')
    .on('click', 'a.delete', function (this: HTMLAnchorElement) {
      const link = this;
      modal({
        content: $('.forum-delete-modal'),
        onInsert($wrap) {
          $wrap
            .find('form')
            .attr('action', link.href)
            .on('submit', function (this: HTMLFormElement, e: Event) {
              e.preventDefault();
              xhr.formToXhr(this);
              modal.close();
              $(link).closest('.forum-post').hide();
            });
        },
      });
      return false;
    })
    .on('click', 'form.unsub button', function (this: HTMLButtonElement) {
      const form = $(this).parent().toggleClass('on off')[0] as HTMLFormElement;
      xhr.text(`${form.action}?unsub=${$(this).data('unsub')}`, { method: 'post' });
      return false;
    });

  $('.forum-post__message').each(function (this: HTMLElement) {
    if (this.innerText.match(/(^|\n)>/)) {
      let result = "";
      let quote = [];
      for (const line of this.innerHTML.split('<br>')) {
        if (line.startsWith('&gt;')) quote.push(line.substring(4));
        else {
          if (quote.length > 0) {
            result += `<blockquote>${quote.join('<br>')}</blockquote>`;
            quote = [];
          }
          result += line + "<br>";
        }
      }
      if (quote.length > 0) result += `<blockquote>${quote.join('<br>')}</blockquote>`;
      this.innerHTML = result;
    }
  });

  $('.edit.button')
    .add('.edit-post-cancel')
    .on('click', function (this: HTMLButtonElement, e) {
      e.preventDefault();

      const $post = $(this).closest('.forum-post'),
        $form = $post.find('form.edit-post-form').toggle();

      ($form[0] as HTMLFormElement).reset();
    });

  $('.quote.button').on('click', function (this: HTMLButtonElement) {
    const $post = $(this).closest('.forum-post'),
      author = $post.find('.author').attr('href')!.substring(3),
      anchor = $post.find('.anchor').text(),
      message = $post.find('.forum-post__message')[0] as HTMLElement,
      response = $('.reply .post-text-area')[0] as HTMLTextAreaElement;

    let quote = message.innerText
      .split('\n')
      .map(line => '> ' + line)
      .join('\n');
    quote = `@${author} said in ${anchor}:\n${quote}\n`;
    response.value =
      response.value.substring(0, response.selectionStart) + quote + response.value.substring(response.selectionEnd);
  });

  $('.post-text-area').one('focus', function (this: HTMLTextAreaElement) {
    const textarea = this,
      topicId = $(this).attr('data-topic');

    if (topicId)
      lichess.loadScript('vendor/textcomplete.min.js').then(function () {
        const searchCandidates = function (term: string, candidateUsers: string[]) {
          return candidateUsers.filter((user: string) => user.toLowerCase().startsWith(term.toLowerCase()));
        };

        // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
        // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
        // is focused.
        const threadParticipants = xhr.json('/forum/participants/' + topicId);

        const textcomplete = new window.Textcomplete(new window.Textcomplete.editors.Textarea(textarea));

        textcomplete.register(
          [
            {
              match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
              search: function (term: string, callback: (names: string[]) => void) {
                // Initially we only autocomplete by participants in the thread. As the user types more,
                // we can autocomplete against all users on the site.
                threadParticipants.then(function (participants) {
                  const forumParticipantCandidates = searchCandidates(term, participants);

                  if (forumParticipantCandidates.length != 0) {
                    // We always prefer a match on the forum thread partcipants' usernames
                    callback(forumParticipantCandidates);
                  } else if (term.length >= 3) {
                    // We fall back to every site user after 3 letters of the username have been entered
                    // and there are no matches in the forum thread participants
                    xhr
                      .json(xhr.url('/player/autocomplete', { term }), { cache: 'default' })
                      .then(candidateUsers => callback(searchCandidates(term, candidateUsers)));
                  } else {
                    callback([]);
                  }
                });
              },
              replace: (mention: string) => '$1@' + mention + ' ',
            },
          ],
          {
            placement: 'top',
            appendTo: '#lichess_forum',
          }
        );
      });
  });

  $('.forum').on('click', '.reactions-auth button', e => {
    const href = e.target.getAttribute('data-href');
    if (href) {
      const $rels = $(e.target).parent();
      if ($rels.hasClass('loading')) return;
      $rels.addClass('loading');
      xhr.text(href, { method: 'post' }).then(
        html => {
          $rels.replaceWith(html);
          $rels.removeClass('loading');
        },
        _ => {
          lichess.announce({ msg: 'Failed to send forum post reaction' });
        }
      );
    }
  });

  const replyStorage = lichess.tempStorage.make('forum.reply' + location.pathname);
  const replyEl = $('.reply .post-text-area')[0] as HTMLTextAreaElement | undefined;
  const storedReply = replyStorage.get();
  let submittingReply = false;
  if (replyEl && storedReply) replyEl.value = storedReply;

  window.addEventListener('unload', () => {
    if (!submittingReply) {
      if (replyEl?.value) replyStorage.set(replyEl.value);
      else replyStorage.remove();
    }
  });

  $('form.reply').on('submit', () => {
    replyStorage.remove();
    submittingReply = true;
  });
});
