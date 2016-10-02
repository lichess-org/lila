$(function() {
  $('.edit.button').add('.edit-post-cancel').click(function(e) {
    e.preventDefault();

    var post = $(this).closest('.post');
    var message = post.find('.message').toggle();
    var form = post.find('form.edit-post-form').toggle();

    form[0].reset();
    form.find('textarea').height(message.height());
  });

  lichess.loadScript('/assets/vendor/jquery-textcomplete/dist/jquery.textcomplete.js').then(function() {

    var getThreadParticipants = function() {
      var topicId = $('.post-text-area').attr('data-topic');

      if (!topicId) {
        return jQuery.Deferred().resolve([]);
      } else {
        return $.ajax({
          url: "/forum/participants/" + topicId
        });
      }
    };

    var searchCandidates = function(term, candidateUsers) {
      return candidateUsers.filter(function(user) {
        return user.toLowerCase().indexOf(term.toLowerCase()) === 0;
      });
    };

    // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
    // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
    // is focused.
    var threadParticipants = Promise.resolve([]);

    $('.post-text-area').textcomplete([{
      match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
      search: function(term, callback) {

        if (term.length < 3) {
          // Initially we only autocomplete by participants in the thread. As the user types more,
          // we can autocomplete against all users on the site.

          threadParticipants.then(function(participants) {
            callback(searchCandidates(term, participants));
          });
        } else {
          $.ajax({
            url: "/player/autocomplete",
            data: {
              term: term
            },
            success: function(candidateUsers) {
              callback(searchCandidates(term, candidateUsers));
            },
            cache: true
          });
        }
      },
      replace: function(mention) {
        return '$1@' + mention + ' ';
      }
    }], {
      placement: 'top',
      appendTo: '#lichess_forum'
    });

    $('.post-text-area').one('focus', function() {
      threadParticipants = Promise.resolve(getThreadParticipants());
    });
  });
});
