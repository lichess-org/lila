window.onload = function() {
  if (!window.lidraughts_challenge) return;
  var opts = lidraughts_challenge;
  var element = document.getElementById('challenge');
  var challenge = opts.data.challenge;
  var accepting;
  lidraughts.socket = new lidraughts.StrongSocket(
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
              $('.lidraughts_overboard').replaceWith($(html).find('.lidraughts_overboard'));
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
    $('.lidraughts_overboard').find('form.accept').submit(function() {
      accepting = true;
      $(this).html('<span class="ddloader"></span>');
    });
    $('.lidraughts_overboard').find('form.xhr').submit(function(e) {
      e.preventDefault();
      $.ajax({
        url: $(this).attr('action'),
        method: 'post'
      });
      $(this).html('<span class="ddloader"></span>');
    });
    $('.lidraughts_overboard').find('input.friend-autocomplete').each(function() {
      var $input = $(this);
      lidraughts.userAutocomplete($input, {
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
      lidraughts.socket.send('ping');
      setTimeout(pingNow, 2000);
    }
  };
  pingNow();

  var ground = Draughtsground(element.querySelector('.lidraughts_board'), {
    viewOnly: true,
    drawable: { enabled: false, visible: false },
    fen: challenge.initialFen,
    orientation: (opts.owner ^ challenge.color === 'black') ? 'white' : 'black',
    coordinates: false,
    disableContextMenu: true
  });
  setTimeout(function() {
    $('.lidraughts_overboard_wrap', element).addClass('visible');
  }, 100);
};
