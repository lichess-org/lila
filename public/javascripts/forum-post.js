$(function () {
  $('.forum')
    .on('click', 'a.delete', function () {
      $.post($(this).attr('href'));
      $(this).closest('.forum-post').hide();
      return false;
    })
    .on('click', 'form.unsub button', function () {
      var $form = $(this).parent().toggleClass('on off');
      $.post($form.attr('action') + '?unsub=' + $(this).data('unsub'));
      return false;
    });

  $('.edit.button')
    .add('.edit-post-cancel')
    .click(function (e) {
      e.preventDefault();

      var post = $(this).closest('.forum-post');
      var message = post.find('.message').toggle();
      var form = post.find('form.edit-post-form').toggle();

      form[0].reset();
      form.find('textarea').height(message.height());
    });

  $('.post-text-area').one('focus', function () {
    var textarea = this,
      topicId = $(this).attr('data-topic');

    if (topicId)
      lishogi.loadScript('vendor/textcomplete.min.js').then(function () {
        var searchCandidates = function (term, candidateUsers) {
          return candidateUsers.filter(function (user) {
            return user.toLowerCase().startsWith(term.toLowerCase());
          });
        };

        // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
        // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
        // is focused.
        var threadParticipants = $.ajax({
          url: '/forum/participants/' + topicId,
        });

        var textcomplete = new Textcomplete(new Textcomplete.editors.Textarea(textarea));

        textcomplete.register(
          [
            {
              match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
              search: function (term, callback) {
                // Initially we only autocomplete by participants in the thread. As the user types more,
                // we can autocomplete against all users on the site.
                threadParticipants.then(function (participants) {
                  var forumParticipantCandidates = searchCandidates(term, participants);

                  if (forumParticipantCandidates.length != 0) {
                    // We always prefer a match on the forum thread partcipants' usernames
                    callback(forumParticipantCandidates);
                  } else if (term.length >= 3) {
                    // We fall back to every site user after 3 letters of the username have been entered
                    // and there are no matches in the forum thread participants
                    $.ajax({
                      url: '/api/player/autocomplete',
                      data: {
                        term: term,
                      },
                      success: function (candidateUsers) {
                        callback(searchCandidates(term, candidateUsers));
                      },
                      cache: true,
                    });
                  } else {
                    callback([]);
                  }
                });
              },
              replace: function (mention) {
                return '$1@' + mention + ' ';
              },
            },
          ],
          {
            placement: 'top',
            appendTo: '#lishogi_forum',
          }
        );
      });
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
        .then(html => $rels.replaceWith(html))
        .catch(() => {
          lishogi.announce({ msg: 'Failed to send forum post reaction' });
        })
        .finally(() => $rels.removeClass('loading'));
    }
  });
});
