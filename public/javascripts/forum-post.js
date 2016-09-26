$(function () {
  $('.edit.button').add('.edit-post-cancel').click(function(e) {
    e.preventDefault();

    var post = $(this).closest('.post');
    var message = post.find('.message').toggle();
    var form = post.find('form.edit-post-form').toggle();

    form[0].reset();
    form.find('textarea').height(message.height());
  });

  lichess.loadScript('/assets/javascripts/vendor/jquery.textcomplete.js').then(function() {

    var getDataRetrievalUrl = function(term) {
        var dataRetrievalUrl = "";

        if (term.length < 3) {
            // Initially we only autocomplete on people who are participating in the thread

            var topicId = $('.post-text-area').attr('data-topic');

            if (!topicId) {
                return null;
            } else {
                return "/forum/participants/" + topicId;
            }
        } else {
            // After 3 characters, we can autocomplete against all users on the site
            return "/player/autocomplete?term=" + term;
        }
    };

    var getTopicId = function () {
        return $('.post-text-area').attr('data-topic');
    };

    var getThreadParticipants = function() {
        var topicId = getTopicId();

        if (!topicId) {
            return jQuery.Deferred().resolve([]);
        } else {
            return $.ajax({url: "/forum/participants/" + topicId});
        }

    };

    var searchCandidates = function(term, candidateUsers) {
        return $.map(candidateUsers,
            function (user) {
                return user.indexOf(term) === 0 ? user : null;
            });
    };


    $('.post-text-area').textcomplete([
         {
             match: /\B@(\w*)$/,
             search: function (term, callback) {

                if (term.length < 3) {
                    // Initially we only autocomplete by participants in the read. As the user types more,
                    // we can autocomplete against all users on the site.

                    var participants = getThreadParticipants();
                    participants.done(function(participants) {
                        callback(searchCandidates(term, participants));
                    });
                } else {
                    $.ajax(
                            {
                                url: "/player/autocomplete?term=" + term,
                                success: function(candidateUsers) {
                                    callback(searchCandidates(term, candidateUsers));
                                }
                            });
                }
             },
             index: 1,
             replace: function (mention) {
                 return '@' + mention + ' ';
             }
         }
        ],
        {
            'placement' : 'top'
        }


        );
  });


});
