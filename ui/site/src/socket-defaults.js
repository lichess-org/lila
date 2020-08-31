lichess.setSocketDefaults = $friendsBox =>
  $.extend(true, lichess.StrongSocket.defaults, {
    events: {
      following_onlines(_, d) {
        d.users = d.d;
        $friendsBox.friends("set", d);
      },
      following_enters(_, d) {
        $friendsBox.friends('enters', d);
      },
      following_leaves(name) {
        $friendsBox.friends('leaves', name);
      },
      following_playing(name) {
        $friendsBox.friends('playing', name);
      },
      following_stopped_playing(name) {
        $friendsBox.friends('stopped_playing', name);
      },
      redirect(o) {
        setTimeout(() => {
          lichess.hasToReload = true;
          lichess.redirect(o);
        }, 200);
      },
      tournamentReminder(data) {
        if ($('#announce').length || $('body').data("tournament-id") == data.id) return;
        const url = '/tournament/' + data.id;
        $('body').append(
          '<div id="announce">' +
          '<a data-icon="g" class="text" href="' + url + '">' + data.name + '</a>' +
          '<div class="actions">' +
          '<a class="withdraw text" href="' + url + '/withdraw" data-icon="Z">Pause</a>' +
          '<a class="text" href="' + url + '" data-icon="G">Resume</a>' +
          '</div></div>'
        ).find('#announce .withdraw').click(function() {
          $.post($(this).attr("href"));
          $('#announce').remove();
          return false;
        });
      },
      announce: lichess.announce
    },
    params: {},
    options: {
      isAuth: !!$('body').data('user')
    }
  });
