lichess = lichess || {};
lichess.startChallenge = function(element, opts) {
  var challenge = opts.data.challenge;
  var accepting;
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
          $.ajax({
            url: opts.xhrUrl,
            success(html) {
              $('.lichess_overboard').replaceWith($(html).find('.lichess_overboard'));
              init();
            }
          });
        }
      }
    });

  var init = function() {
    if (!accepting) $('#challenge_redirect').each(function() {
      location.href = $(this).attr('href');
    });
    $('.lichess_overboard').find('form.accept').submit(function() {
      accepting = true;
      $(this).html(lichess.spinnerHtml);
    });
    $('.lichess_overboard').find('form.xhr').submit(function(e) {
      e.preventDefault();
      $.ajax({
        url: $(this).attr('action'),
        method: 'post'
      });
      $(this).html(lichess.spinnerHtml);
    });
  };
  init();

  var pingNow = function() {
    if (document.getElementById('ping_challenge')) {
      lichess.socket.send('ping');
      setTimeout(pingNow, 2000);
    }
  };
  pingNow();

  var ground = Chessground(element.querySelector('.lichess_board'), {
    viewOnly: true,
    fen: challenge.initialFen,
    orientation: (opts.owner ^ challenge.color === 'black') ? 'white' : 'black',
    coordinates: false,
    disableContextMenu: true
  });
  setTimeout(function() {
    $('.lichess_overboard_wrap', element).addClass('visible');
  }, 100);
};
