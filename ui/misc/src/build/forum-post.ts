import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

window.lishogi.ready.then(() => {
  $('.forum')
    .on('click', 'a.delete', function (this: HTMLAnchorElement) {
      window.lishogi.xhr.text('POST', this.href);
      $(this).closest('.forum-post').hide();
      return false;
    })
    .on('click', 'form.unsub button', function (this: HTMLButtonElement) {
      const form = $(this).parent().toggleClass('on off')[0] as HTMLFormElement;
      window.lishogi.xhr.text('POST', form.action, {
        formData: {
          unsub: this.dataset.unsub,
        },
      });
      return false;
    });

  $('.edit.button')
    .add('.edit-post-cancel')
    .click(function (e) {
      e.preventDefault();

      const post = $(this).closest('.forum-post');
      const message = post.find('.message').toggle();
      const form = post.find('form.edit-post-form').toggle();

      (form[0] as HTMLFormElement).reset();
      form.find('textarea').height(message.height());
    });

  $('.post-text-area').one('focus', function (this: HTMLTextAreaElement) {
    const textarea = this,
      topicId = $(this).attr('data-topic');

    if (!topicId) return;

    const searchCandidates = function (term: string, candidateUsers: string[]) {
      return candidateUsers.filter((user: string) =>
        user.toLowerCase().startsWith(term.toLowerCase()),
      );
    };

    // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
    // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
    // is focused.
    const threadParticipants = window.lishogi.xhr.json('GET', '/forum/participants/' + topicId);

    new Textcomplete(new TextareaEditor(textarea), [
      {
        index: 2,
        match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
        search: function (term: string, callback: (names: string[]) => void) {
          // Initially we only autocomplete by participants in the thread. As the user types more,
          // we can autocomplete against all users on the site.
          threadParticipants.then(participants => {
            const forumParticipantCandidates = searchCandidates(term, participants);

            if (forumParticipantCandidates.length != 0) {
              // We always prefer a match on the forum thread participants' usernames
              callback(forumParticipantCandidates);
            } else if (term.length >= 3) {
              // We fall back to every site user after 3 letters of the username have been entered
              // and there are no matches in the forum thread participants

              window.lishogi.xhr
                .json(
                  'GET',
                  '/api/player/autocomplete',
                  { url: { term } },
                  {
                    cache: 'default',
                  },
                )
                .then(candidateUsers => callback(searchCandidates(term, candidateUsers)))
                .catch(error => {
                  console.error('Autocomplete request failed:', error);
                  callback([]);
                });
            } else {
              callback([]);
            }
          });
        },
        replace: (mention: string) => '$1@' + mention + ' ',
      },
    ]);
  });

  $('.forum').click('.reactions-auth button', e => {
    const href = e.target.getAttribute('data-href');
    if (href) {
      const $rels = $(e.target).parent();
      if ($rels.hasClass('loading')) return;
      $rels.addClass('loading');
      fetch(href, { method: 'post', credentials: 'same-origin' })
        .then(res => {
          if (res.ok) return res.text();
          else throw res.statusText;
        })
        .then(html => {
          $rels.replaceWith(html);
          $rels.removeClass('loading');
        })
        .catch(() => {
          window.lishogi.announce({ msg: 'Failed to send forum post reaction' });
          $rels.removeClass('loading');
        });
    }
  });

  document.querySelectorAll<HTMLImageElement>('.forum-post__message img.embed').forEach(img => {
    img.addEventListener('click', event => {
      const target = event.currentTarget as HTMLImageElement;
      if (target.src) {
        window.open(target.src, '_blank');
      }
    });
  });
});
