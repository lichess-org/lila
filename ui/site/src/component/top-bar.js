lichess.topBar = () => {

  const initiatingHtml = '<div class="initiating">' + lichess.spinnerHtml + '</div>';

  $('#topnav-toggle').on('change', e => {
    document.body.classList.toggle('masked', e.target.checked);
  });

  $('#top').on('click', 'a.toggle', function() {
    var $p = $(this).parent();
    $p.toggleClass('shown');
    $p.siblings('.shown').removeClass('shown');
    lichess.pubsub.emit('top.toggle.' + $(this).attr('id'));
    setTimeout(function() {
      const handler = function(e) {
        if ($.contains($p[0], e.target)) return;
        $p.removeClass('shown');
        $('html').off('click', handler);
      };
      $('html').on('click', handler);
    }, 10);
    return false;
  });

  { // challengeApp
    let instance, booted;
    const $toggle = $('#challenge-toggle');
    $toggle.one('mouseover click', () => load());
    const load = function(data) {
      if (booted) return;
      booted = true;
      const $el = $('#challenge-app').html(initiatingHtml);
      lichess.loadCssPath('challenge');
      lichess.loadScript(lichess.jsModule('challenge')).done(function() {
        instance = LichessChallenge($el[0], {
          data: data,
          show() {
            if (!$('#challenge-app').is(':visible')) $toggle.click();
          },
          setCount(nb) {
            $toggle.find('span').attr('data-count', nb);
          },
          pulse() {
            $toggle.addClass('pulse');
          }
        });
      });
    };
    lichess.challengeApp = {
      update(data) {
        if (!instance) load(data);
        else instance.update(data);
      },
      open() {
        $toggle.click();
      }
    };
  }

  { // notifyApp
    let instance, booted;
    const $toggle = $('#notify-toggle'),
      isVisible = () => $('#notify-app').is(':visible');

    const load = function(data, incoming) {
      if (booted) return;
      booted = true;
      var $el = $('#notify-app').html(initiatingHtml);
      lichess.loadCssPath('notify');
      lichess.loadScript(lichess.jsModule('notify')).done(() => {
        instance = LichessNotify($el.empty()[0], {
          data: data,
          incoming: incoming,
          isVisible: isVisible,
          setCount(nb) {
            $toggle.find('span').attr('data-count', nb);
          },
          show() {
            if (!isVisible()) $toggle.click();
          },
          setNotified() {
            lichess.socket.send('notified');
          },
          pulse() {
            $toggle.addClass('pulse');
          }
        });
      });
    };

    $toggle.one('mouseover click', () => load()).click(() => {
      if ('Notification' in window) Notification.requestPermission();
      setTimeout(() => {
        if (instance && isVisible()) instance.setVisible();
      }, 200);
    });

    lichess.notifyApp = {
      update(data, incoming) {
        if (!instance) load(data, incoming);
        else instance.update(data, incoming);
      },
      setMsgRead(user) {
        if (!instance) load();
        else instance.setMsgRead(user);
      }
    };
  }

  { // dasher
    let booted;
    $('#top .dasher .toggle').one('mouseover click', function() {
      if (booted) return;
      booted = true;
      const $el = $('#dasher_app').html(initiatingHtml),
        playing = $('body').hasClass('playing');
      lichess.loadCssPath('dasher');
      lichess.loadScript(lichess.jsModule('dasher')).done(() =>
        LichessDasher($el.empty()[0], {
          playing
        })
      );
    });
  }

  { // cli
    const $wrap = $('#clinput');
    if (!$wrap.length) return;
    let booted;
    const $input = $wrap.find('input');
    const boot = () => {
      if (booted) return;
      booted = true;
      lichess.loadScript(lichess.jsModule('cli')).done(() =>
        LichessCli.app($wrap, toggle)
      );
    };
    const toggle = () => {
      boot();
      $('body').toggleClass('clinput');
      if ($('body').hasClass('clinput')) $input.focus();
    };
    $wrap.find('a').on('mouseover click', e => (e.type === 'mouseover' ? boot : toggle)());
    Mousetrap.bind('/', () => {
      $input.val('/');
      requestAnimationFrame(() => toggle());
      return false;
    });
    Mousetrap.bind('s', () => requestAnimationFrame(() => toggle()));
    if ($('body').hasClass('blind-mode')) $input.one('focus', () => toggle());
  }
}
