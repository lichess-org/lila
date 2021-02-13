import pubsub from './pubsub';
import spinnerHtml from './spinner';
import { loadCssPath, loadScript, jsModule } from './assets';

export default function() {

  const initiatingHtml = `<div class="initiating">${spinnerHtml}</div>`;

  $('#topnav-toggle').on('change', e =>
    document.body.classList.toggle('masked', (e.target as HTMLInputElement).checked)
  );

  $('#top').on('click', 'a.toggle', function(this: HTMLElement) {
    var $p = $(this).parent();
    $p.toggleClass('shown');
    $p.siblings('.shown').removeClass('shown');
    pubsub.emit('top.toggle.' + $(this).attr('id'));
    setTimeout(() => {
      const handler = (e: Event) => {
        if ($.contains($p[0], e.target as HTMLElement)) return;
        $p.removeClass('shown');
        $('html').off('click', handler);
      };
      $('html').on('click', handler);
    }, 10);
    return false;
  });

  { // challengeApp
    let instance, booted: boolean;
    const $toggle = $('#challenge-toggle');
    $toggle.one('mouseover click', () => load());
    const load = function(data?: any) {
      if (booted) return;
      booted = true;
      const $el = $('#challenge-app').html(initiatingHtml);
      loadCssPath('challenge');
      loadScript(jsModule('challenge')).then(() =>
        instance = window.LichessChallenge($el[0], {
          data,
          show() {
            if (!$('#challenge-app').is(':visible')) $toggle.click();
          },
          setCount(nb: number) {
            $toggle.find('span').attr('data-count', nb);
          },
          pulse() {
            $toggle.addClass('pulse');
          }
        })
      );
    };
    pubsub.on('socket.in.challenges', data => {
      if (!instance) load(data);
      else instance.update(data);
    });
    pubsub.on('challenge-app.open', () => $toggle.click());
  }

  { // notifyApp
    let instance, booted: boolean;
    const $toggle = $('#notify-toggle'),
      isVisible = () => $('#notify-app').is(':visible');

    const load = (data?: any, incoming = false) => {
      if (booted) return;
      booted = true;
      var $el = $('#notify-app').html(initiatingHtml);
      loadCssPath('notify');
      loadScript(jsModule('notify')).then(() =>
        instance = window.LichessNotify($el.empty()[0], {
          data,
          incoming,
          isVisible,
          setCount(nb: number) {
            $toggle.find('span').attr('data-count', nb);
          },
          show() {
            if (!isVisible()) $toggle.click();
          },
          setNotified() {
            window.lichess.socket.send('notified');
          },
          pulse() {
            $toggle.addClass('pulse');
          }
        })
      );
    };

    $toggle.one('mouseover click', () => load()).click(() => {
      if ('Notification' in window) Notification.requestPermission();
      setTimeout(() => {
        if (instance && isVisible()) instance.setVisible();
      }, 200);
    });

    pubsub.on('socket.in.notifications', data => {
      if (!instance) load(data, true);
      else instance.update(data, true);
    });
    pubsub.on('notify-app.set-read', user => {
      if (!instance) load();
      else instance.setMsgRead(user);
    });
  }

  { // dasher
    let booted: boolean;
    $('#top .dasher .toggle').one('mouseover click', function() {
      if (booted) return;
      booted = true;
      const $el = $('#dasher_app').html(initiatingHtml),
        playing = $('body').hasClass('playing');
      loadCssPath('dasher');
      loadScript(jsModule('dasher')).then(() =>
        window.LichessDasher($el.empty()[0], {
          playing
        })
      );
    });
  }

  { // cli
    const $wrap = $('#clinput');
    if (!$wrap.length) return;
    let booted: boolean;
    const $input = $wrap.find('input');
    const boot = () => {
      if (booted) return;
      booted = true;
      loadScript(jsModule('cli')).then(() =>
        window.LichessCli.app($wrap, toggle)
      );
    };
    const toggle = () => {
      boot();
      $('body').toggleClass('clinput');
      if ($('body').hasClass('clinput')) $input.focus();
    };
    $wrap.find('a').on('mouseover click', e => (e.type === 'mouseover' ? boot : toggle)());
    window.Mousetrap.bind('/', () => {
      $input.val('/');
      requestAnimationFrame(() => toggle());
      return false;
    });
    window.Mousetrap.bind('s', () => requestAnimationFrame(() => toggle()));
    if ($('body').hasClass('blind-mode')) $input.one('focus', () => toggle());
  }
}
