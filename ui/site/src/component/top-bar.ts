import pubsub from './pubsub';
import { loadCssPath, loadEsm } from './assets';
import { memoize } from 'common';

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

  $('#tn-tg').on('change', e =>
    document.body.classList.toggle('masked', (e.target as HTMLInputElement).checked),
  );

  $('#top').on('click', '.toggle', function (this: HTMLElement) {
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
    let instance: Promise<any> | undefined;
    const $toggle = $('#challenge-toggle'),
      $countSpan = $toggle.find('span');
    $toggle.one('mouseover click', () => load());
    const load = function (data?: any) {
      if (instance) return;
      const $el = $('#challenge-app').html(initiatingHtml);
      loadCssPath('challenge');
      instance = loadEsm('challenge', {
        init: {
          el: $el[0],
          data,
          show() {
            if (!isVisible('#challenge-app')) $toggle.trigger('click');
          },
          setCount(nb: number) {
            const newTitle = $countSpan.attr('title')!.replace(/\d+/, nb.toString());
            $countSpan.data('count', nb).attr('title', newTitle).attr('aria-label', newTitle);
          },
          pulse() {
            $toggle.addClass('pulse');
          },
        },
      });
    };
    pubsub.on('socket.in.challenges', async data => {
      if (!instance) load(data);
      else (await instance).update(data);
    });

    pubsub.on('challenge-app.open', () => $toggle.trigger('click'));
  }

  {
    // notifyApp
    let instance: Promise<any> | undefined;
    const $toggle = $('#notify-toggle'),
      $countSpan = $toggle.find('span'),
      selector = '#notify-app';

    const load = (data?: any) => {
      if (instance) return;
      const $el = $('#notify-app').html(initiatingHtml);
      loadCssPath('notify');
      instance = loadEsm('notify', {
        init: {
          el: $el.empty()[0],
          data,
          isVisible: () => isVisible(selector),
          updateUnread(nb: number | 'increment') {
            const existing = ($countSpan.data('count') as number) || 0;
            if (nb == 'increment') nb = existing + 1;
            if (this.isVisible()) nb = 0;
            const newTitle = $countSpan.attr('title')!.replace(/\d+/, nb.toString());
            $countSpan.data('count', nb).attr('title', newTitle).attr('aria-label', newTitle);
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
        },
      });
    };

    $toggle
      .one('mouseover click', () => load())
      .on('click', () => {
        if ('Notification' in window) Notification.requestPermission();
        setTimeout(async () => {
          if (instance && isVisible(selector)) (await instance).onShow();
        }, 200);
      });

    pubsub.on('socket.in.notifications', async data => {
      if (!instance) load(data);
      else (await instance).update(data);
    });
    pubsub.on('notify-app.set-read', async user => {
      if (!instance) load();
      else (await instance).setMsgRead(user);
    });
  }

  {
    // dasher
    const load = memoize(() => loadEsm('dasher'));
    $('#top .dasher .toggle').one('mouseover click', function (this: HTMLElement) {
      $(this).removeAttr('href');
      loadCssPath('dasher');
      load();
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
      loadEsm('cli', { init: { input: $input[0] } }).catch(() => (booted = false));
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
    lichess.mousetrap
      .bind('/', () => {
        $input.val('/');
        $input[0]!.focus();
      })
      .bind('s', () => $input[0]!.focus());
  }
}
