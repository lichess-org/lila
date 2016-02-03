lichess = lichess || {};
lichess.startChallenge = function(element, opts) {
  console.log(opts);
  var challenge = opts.data.challenge;
  if (!opts.owner && lichess.openInMobileApp(challenge.id)) return;
  lichess.socket = new lichess.StrongSocket(
    opts.socketUrl,
    opts.data.socketVersion, {
      options: {
        name: "challenge"
      },
      params: {
        ran: "--ranph--"
      },
      events: {
        reload: function() {
          console.log('reload');
          $.ajax({
            url: opts.xhrUrl,
            success(html) {
              $('.lichess_overboard').replaceWith($(html).find('.lichess_overboard'));
              $('#challenge_redirect').each(function() {
                location.href = $(this).attr('href');
              });
            }
          });
        }
      }
    });

  var pingNow = function() {
    if (document.getElementById('ping_challenge')) {
      lichess.socket.send('ping');
      setTimeout(pingNow, 2000);
    }
  };
  pingNow();

  Chessground(element.querySelector('.lichess_board'), {
    viewOnly: true,
    fen: challenge.initialFen,
    orientation: challenge.color,
    coordinates: false,
    disableContextMenu: true
  });
  setTimeout(function() {
    $('.lichess_overboard_wrap', element).addClass('visible');
  }, 100);
};
