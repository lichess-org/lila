window.onload = function() {
  if (!window.lichess_challenge) return;
  var opts = lichess_challenge;
  var element = document.getElementById('challenge');
  var challenge = opts.data.challenge;
  var accepting;
  lichess.socket = new lichess.StrongSocket(
    opts.socketUrl,
    opts.data.socketVersion, {
      options: {
        name: "challenge"
      },
      events: {
        reload: function() {
          $.ajax({
            url: opts.xhrUrl,
            success: function(html) {
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
      $(this).html('<span class="ddloader"></span>');
    });
    $('.lichess_overboard').find('form.xhr').submit(function(e) {
      e.preventDefault();
      $.ajax({
        url: $(this).attr('action'),
        method: 'post'
      });
      $(this).html('<span class="ddloader"></span>');
    });
    $('.lichess_overboard').find('input.friend-autocomplete').each(function() {
      var $input = $(this);
      lichess.userAutocomplete($input, {
        focus: 1,
        friend: 1,
        tag: 'span',
        onSelect: function() {
          $input.parents('form').submit();
        }
      });
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
    drawable: { enabled: false, visible: false },
    fen: challenge.initialFen,
    orientation: (opts.owner ^ challenge.color === 'black') ? 'white' : 'black',
    coordinates: false,
    disableContextMenu: true
  });
  setTimeout(function() {
    $('.lichess_overboard_wrap', element).addClass('visible');
  }, 100);
};
