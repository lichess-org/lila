import pubsub from './pubsub';
import { loadCssPath, loadModule } from './assets';
import { loadDasher } from 'common/dasher';

export default function () {
  const initiatingHtml = `<div class="initiating">${lichess.spinnerHtml}</div>`,
    isVisible = (selector: string) => {
      const el = document.querySelector(selector),
        display = el && window.getComputedStyle(el).display;
      return display && display != 'none';
    };

  // On touchscreens, clicking the top menu element expands it. There's no top link.
  // Only for $mq-topnav-hidden in ui/common/css/abstract/_media-queries.scss
  if ('ontouchstart' in window && !window.matchMedia('(max-width: 979px)').matches)
    $('#topnav > section > a').removeAttr('href');

  $('#topnav-toggle').on('change', e =>
    document.body.classList.toggle('masked', (e.target as HTMLInputElement).checked)
  );

  $('#top').on('click', 'a.toggle', function (this: HTMLElement) {
    const $p = $(this).parent().toggleClass('shown');
    $p.siblings('.shown').removeClass('shown');
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

  {
    // challengeApp
    let instance: any, booted: boolean;
    const $toggle = $('#challenge-toggle');
    $toggle.one('mouseover click', () => load());
    const load = function (data?: any) {
      if (booted) return;
      booted = true;
      const $el = $('#challenge-app').html(initiatingHtml);
      loadCssPath('challenge');
      loadModule('challenge').then(
        () =>
          (instance = window.LichessChallenge($el[0], {
            data,
            show() {
              if (!isVisible('#challenge-app')) $toggle.trigger('click');
            },
            setCount(nb: number) {
              $toggle.find('span').data('count', nb);
            },
            pulse() {
              $toggle.addClass('pulse');
            },
          }))
      );
    };
    pubsub.on('socket.in.challenges', data => {
      if (!instance) load(data);
      else instance.update(data);
    });
    pubsub.on('challenge-app.open', () => $toggle.trigger('click'));
  }

  {
    // notifyApp
    let instance: any, booted: boolean;
    const $toggle = $('#notify-toggle'),
      selector = '#notify-app';

    const load = (data?: any) => {
      if (booted) return;
      booted = true;
      const $el = $('#notify-app').html(initiatingHtml);
      loadCssPath('notify');
      loadModule('notify').then(() => {
        instance = window.LichessNotify($el.empty()[0], {
          data,
          isVisible: () => isVisible(selector),
          updateUnread(nb: number | 'increment') {
            const existing = ($toggle.find('span').data('count') as number) || 0;
            if (nb == 'increment') nb = existing + 1;
            $toggle.find('span').data('count', this.isVisible() ? 0 : nb);
            return nb && nb != existing;
          },
          show() {
            if (!isVisible(selector)) $toggle.trigger('click');
          },
          setNotified() {
            lichess.socket.send('notified');
          },
          pulse() {
            $toggle.addClass('pulse');
          },
        });
      });
    };

    $toggle
      .one('mouseover click', () => load())
      .on('click', () => {
        if ('Notification' in window) Notification.requestPermission();
        setTimeout(() => {
          if (instance && isVisible(selector)) instance.onShow();
        }, 200);
      });

    pubsub.on('socket.in.notifications', data => {
      if (!instance) load(data);
      else instance.update(data);
    });
    pubsub.on('notify-app.set-read', user => {
      if (!instance) load();
      else instance.setMsgRead(user);
    });
  }

  {
    // dasher
    $('#top .dasher .toggle').one('mouseover click', function (this: HTMLElement) {
      $(this).removeAttr('href');
      loadCssPath('dasher');
      loadDasher();
    });
  }

  {
    // cli
    const $wrap = $('#clinput');
    if (!$wrap.length) return;
    const $input = $wrap.find('input');
    let booted = false;
    const boot = () => {
      if (booted) return;
      booted = true;
      loadModule('cli').then(
        () => window.LichessCli.app($input[0]),
        () => (booted = false)
      );
    };
    $input.on({
      blur() {
        $input.val('');
        $('body').removeClass('clinput');
      },
      focus() {
        boot();
        $('body').addClass('clinput');
      },
    });
    $wrap.find('a').on({
      mouseover: boot,
      click() {
        $('body').hasClass('clinput') ? $input[0]!.blur() : $input[0]!.focus();
      },
    });
    window.Mousetrap.bind('/', () => {
      $input.val('/');
      $input[0]!.focus();
    }).bind('s', () => $input[0]!.focus());
  }
}
