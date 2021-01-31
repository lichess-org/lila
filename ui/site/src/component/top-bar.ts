import pubsub from './pubsub';
import spinnerHtml from './spinner';
import { loadCssPath, loadModule } from './assets';

export default function() {

  const initiatingHtml = `<div class="initiating">${spinnerHtml}</div>`,
    isVisible = (selector: string) => {
      const el = document.querySelector(selector),
        display = el && window.getComputedStyle(el).display;
      return display && display != 'none';
    };

  $('#topnav-toggle').on('change', e =>
    document.body.classList.toggle('masked', (e.target as HTMLInputElement).checked)
  );

  $('#top').on('click', 'a.toggle', function(this: HTMLElement) {
    const $p = $(this).parent().toggleClass('shown');
    $p.siblings('.shown').removeClass('shown');
    pubsub.emit('top.toggle.' + this.id);
    setTimeout(() => {
      const handler = (e: Event) => {
        if ($p[0]?.contains(e.target as HTMLElement)) return;
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
      loadModule('challenge').then(() =>
        instance = window.LichessChallenge($el[0], {
          data,
          show() {
            if (!isVisible('#challenge-app')) $toggle.trigger('click');
          },
          setCount(nb: number) {
            $toggle.find('span').data('count', nb);
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
    pubsub.on('challenge-app.open', () => $toggle.trigger('click'));
  }

  { // notifyApp
    let instance, booted: boolean;
    const $toggle = $('#notify-toggle'),
      selector = '#notify-app';

    const load = (data?: any, incoming = false) => {
      if (booted) return;
      booted = true;
      const $el = $('#notify-app').html(initiatingHtml);
      loadCssPath('notify');
      loadModule('notify').then(() =>
        instance = window.LichessNotify($el.empty()[0], {
          data,
          incoming,
          isVisible: () => isVisible(selector),
          setCount(nb: number) {
            $toggle.find('span').data('count', nb);
          },
          show() {
            if (!isVisible(selector)) $toggle.trigger('click');
          },
          setNotified() {
            lichess.socket.send('notified');
          },
          pulse() {
            $toggle.addClass('pulse');
          }
        })
      );
    };

    $toggle.one('mouseover click', () => load()).on('click', () => {
      if ('Notification' in window) Notification.requestPermission();
      setTimeout(() => {
        if (instance && isVisible(selector)) instance.setVisible();
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
    $('#top .dasher .toggle').one('mouseover click', function (this: HTMLElement) {
      if (booted) return;
      booted = true;
      $(this).removeAttr('href');
      const $el = $('#dasher_app').html(initiatingHtml),
        playing = $('body').hasClass('playing');
      loadCssPath('dasher');
      loadModule('dasher').then(() => window.LichessDasher($el.empty()[0], { playing }));
    });
  }

  { // cli
    const $wrap = $('#clinput');
    if (!$wrap.length) return;
    const $input = $wrap.find('input');
    let booted = false;
    const boot = () => {
      if (booted) return;
      booted = true;
      loadModule('cli').then(() => window.LichessCli.app($input[0]), () => booted = false);
    };
    $input.on({
      blur() {
        $input.val('');
        $('body').removeClass('clinput');
      },
      focus() {
        boot();
        $('body').addClass('clinput');
      }
    });
    $wrap.find('a').on({
      mouseover: boot,
      click() {
        $('body').hasClass('clinput') ? $input[0]!.blur() : $input[0]!.focus();
      }
    });
    window.Mousetrap
      .bind('/', () => {
        $input.val('/');
        $input[0]!.focus();
      })
      .bind('s', () => $input[0]!.focus());
  }
}
