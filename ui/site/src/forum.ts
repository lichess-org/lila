import * as xhr from 'common/xhr';
import modal from 'common/modal';

lichess.load.then(() => {
  $('.forum')
    .on('click', 'a.delete', function (this: HTMLAnchorElement) {
      console.log(this);
      const $wrap = modal($('.forum-delete-modal'));
      $wrap.find('form').attr('action', this.href);
      // xhr.text(this.href, { method: 'post' });
      // $(this).closest('.forum-post').hide();
      return false;
    })
    .on('click', 'form.unsub button', function (this: HTMLButtonElement) {
      const form = $(this).parent().toggleClass('on off')[0] as HTMLFormElement;
      xhr.text(`${form.action}?unsub=${$(this).data('unsub')}`, { method: 'post' });
      return false;
    });

  $('.edit.button')
    .add('.edit-post-cancel')
    .on('click', function (this: HTMLButtonElement, e) {
      e.preventDefault();

      const post = $(this).closest('.forum-post'),
        message = post.find('.message').toggle(),
        form = post.find('form.edit-post-form').toggle();

      (form[0] as HTMLFormElement).reset();
      form.find('textarea').height(message.height());
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
              search: function (term: string, callback) {
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
              replace: mention => '$1@' + mention + ' ',
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
});
