import { assetUrl, compiledScriptPath, loadCompiledScript, loadCssPath } from 'common/assets';
import { initAll as initMiniBoards, update as updateMiniBoard } from 'common/mini-board';
import { announce } from './announce';
import { challengeApp } from './challenge';
import { loadInfiniteScroll } from './infinite-scroll';
import { fillJquery } from './jquery';
import { mousetrap } from './mousetrap';
import { redirect, reload } from './navigation';
import { notifyApp } from './notify';
import { pubsub } from './pubsub';
import { StrongSocket } from './socket';
import { storage } from './storage';
import * as timeago from './timeago';
import { userAutocomplete } from './user-autocomplete';
import { initiatingHtml } from './util';
import { initWidgets } from './widget';

export function init(): void {
  fillJquery();
  initWidgets();

  requestAnimationFrame(() => {
    initMiniBoards();
    pubsub.on('content_loaded', initMiniBoards);
    pubsub.on('socket.in.sfen', (e: { id: string; sfen: Sfen; lm: Usi }) => {
      const els = document.querySelectorAll<HTMLElement>(`.mini-board-${e.id}`);
      if (els.length) {
        els.forEach(el => {
          el.dataset.sfen = e.sfen;
          updateMiniBoard(el, e.sfen, e.lm);
        });
      }
    });

    function renderTimeago() {
      requestAnimationFrame(() =>
        timeago.render([].slice.call(document.getElementsByClassName('timeago'), 0, 99)),
      );
    }
    function setTimeago(interval) {
      renderTimeago();
      setTimeout(() => setTimeago(interval * 1.1), interval);
    }
    setTimeago(1000);
    pubsub.on('content_loaded', renderTimeago);

    function renderFlatpickr() {
      if (window.flatpickr) {
        document
          .querySelectorAll('.flatpickr--init')
          .forEach(el => window.flatpickr(el, { enableTime: true, time_24hr: true }));
      }
    }
    setTimeout(renderFlatpickr, 200);
    pubsub.on('content_loaded', renderFlatpickr);
  });

  setTimeout(() => {
    announce($('body').data('announce')); // server announcment
    pubsub.on('socket.in.announce', announce);

    $('#friend_box').friends();
    $('.chat__members').watchers();

    $('#main-wrap')
      .on('click', '.autoselect', function (this: HTMLElement) {
        $(this).trigger('select');
      })
      .on('click', 'button.copy', function (this: HTMLElement) {
        $(`#${$(this).data('rel')}`).trigger('select');
        const targetId = $(this).data('rel');
        const textToCopy = $(`#${targetId}`).val() as string;
        navigator.clipboard
          .writeText(textToCopy)
          .then(() => {
            $(this).attr('data-icon', 'E');
          })
          .catch(err => {
            console.error('Failed to copy text: ', err);
          });
      });

    $('body').on('click', 'a.relation-button', function (this: HTMLAnchorElement) {
      const $a = $(this).addClass('processing').css('opacity', 0.3);
      window.lishogi.xhr.text('POST', this.href).then(html => {
        if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
      return false;
    });

    $('.mselect .button').on('click', function (this: HTMLElement) {
      const $p = $(this).parent();
      $p.toggleClass('shown');
      setTimeout(() => {
        const handler = e => {
          if ($.contains($p[0], e.target)) return;
          $p.removeClass('shown');
          $('html').off('click', handler);
        };
        $('html').on('click', handler);
      }, 10);
    });

    document.body.addEventListener('mouseover', window.lishogi.powertip.mouseover);

    window.addEventListener('resize', () =>
      document.body.dispatchEvent(new Event('shogiground.resize')),
    );

    window.lishogi.challengeApp = challengeApp();
    pubsub.on('socket.in.challenges', (d: any) => {
      window.lishogi.challengeApp?.update(d);
    });
    window.lishogi.notifyApp = notifyApp();
    pubsub.on('socket.in.notifications', (d: any) => {
      window.lishogi.notifyApp?.update(d, true);
    });

    // dasher
    {
      let booted = false;

      $('#top .dasher .toggle').one('mouseover click', () => {
        if (booted) return;
        booted = true;
        const $dasher = $('#dasher_app');
        $dasher.html(initiatingHtml);
        const playing = $('body').hasClass('playing');
        loadCssPath('dasher');
        loadCompiledScript('dasher').then(() => {
          $dasher.empty();
          window.lishogi.modules.dasher!({ playing });
        });
      });
    }

    // cli
    {
      const $wrap = $('#clinput');
      if (!$wrap.length) return;
      let booted = false;
      const $input = $wrap.find('input');
      const boot = () => {
        if (booted) return;
        booted = true;
        loadCompiledScript('misc.cli').then(() =>
          window.lishogi.modules.miscCli!({ $wrap, toggle }),
        );
      };
      const toggle = () => {
        boot();
        $('body').toggleClass('clinput');
        if ($('body').hasClass('clinput')) $input.trigger('focus');
      };
      $wrap.find('a').on('mouseover click', e => (e.type === 'mouseover' ? boot : toggle)());
      mousetrap.bind('/', () => {
        $input.val('/');
        requestAnimationFrame(() => toggle());
        return false;
      });
      mousetrap.bind('s', () => requestAnimationFrame(() => toggle()));
      if ($('body').hasClass('blind-mode')) $input.one('focus', () => toggle());
    }

    $('.user-autocomplete').each(function (this: HTMLElement) {
      const opts = {
        focus: true,
        friend: $(this).data('friend'),
        tag: $(this).data('tag'),
      };
      if ($(this).attr('autofocus')) userAutocomplete($(this), opts);
      else
        $(this).one('focus', function (this: HTMLElement) {
          userAutocomplete($(this), opts);
        });
    });

    $('#topnav-toggle').on('change', e => {
      const el = e.target as HTMLInputElement;
      document.body.classList.toggle('masked', el.checked);
    });

    loadInfiniteScroll('.infinitescroll');

    $('#top').on('click', 'a.toggle', function (this: HTMLElement) {
      const $p = $(this).parent();
      $p.toggleClass('shown');
      $p.siblings('.shown').removeClass('shown');
      window.lishogi.pubsub.emit(`top.toggle.${$(this).attr('id')}`);
      setTimeout(() => {
        const handler: (e: JQuery.ClickEvent) => void = (e: JQuery.ClickEvent) => {
          if (
            $.contains($p[0], e.target as HTMLElement) ||
            !!$('.sp-container:not(.sp-hidden)').length
          )
            return;
          $p.removeClass('shown');
          $('html').off('click', handler);
        };
        $('html').on('click', handler);
      }, 10);
      return false;
    });
    $('a.delete, input.delete').on('click', () => confirm('Delete?'));
    $('input.confirm, button.confirm').on('click', function (this: HTMLElement) {
      return confirm($(this).attr('title') || 'Confirm this action?');
    });

    $('#main-wrap').on('click', 'a.bookmark', function (this: HTMLAnchorElement) {
      const t = $(this).toggleClass('bookmarked');
      window.lishogi.xhr.text('POST', this.href);
      const count = (Number.parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
      t.find('span').html(count > 0 ? count.toString() : '');
      return false;
    });

    mousetrap.bind('esc', () => {
      const $oc = $('#modal-wrap .close');
      if ($oc.length) $oc.trigger('click');
      else {
        const $input = $(':focus');
        if ($input.length) $input.trigger('blur');
      }
      return false;
    });

    if (!storage.get('grid'))
      setTimeout(() => {
        if (getComputedStyle(document.body).getPropertyValue('--grid')) storage.set('grid', '1');
        else
          window.lishogi.xhr
            .text('GET', assetUrl('oops/browser.html'))
            .then(html => $('body').prepend(html));
      }, 3000);

    // edge hack, maybe remove?
    // if (navigator.userAgent.indexOf('Edge/') > -1)
    //   setTimeout(function () {
    //     const sprite = $('#piece-sprite');
    //     sprite.attr('href', sprite.attr('href').replace('.css', '.external.css'));

    //     const chuSprite = $('#chu-piece-sprite');
    //     if (chuSprite.length) chuSprite.attr('href', chuSprite.attr('href').replace('.css', '.external.css'));

    //     const kyoSprite = $('#kyo-piece-sprite');
    //     if (kyoSprite.length) kyoSprite.attr('href', kyoSprite.attr('href').replace('.css', '.external.css'));
    //   }, 1000);

    // prevent zoom when keyboard shows on iOS
    if (/iPad|iPhone|iPod/.test(navigator.userAgent) && !(window as any).MSStream) {
      const el = document.querySelector('meta[name=viewport]')!;
      el.setAttribute('content', `${el.getAttribute('content')},maximum-scale=1.0`);
    }

    if (location.hash === '#blind' && !$('body').hasClass('blind-mode'))
      window.lishogi.xhr
        .text('POST', '/toggle-blind-mode', {
          formData: {
            enable: 1,
            redirect: '/',
          },
        })
        .then(reload);
  });

  pubsub.on('socket.in.redirect', d => {
    window.lishogi.properReload = true;
    redirect(d);
  });

  pubsub.on('socket.in.tournamentReminder', (data: any) => {
    if ($('#announce').length || $('body').data('tournament-id') == data.id) return;
    const url = `/tournament/${data.id}`;
    $('body')
      .append(
        `<div id="announce"><a data-icon="g" class="text" href="${url}">${data.name}</a><div class="actions"><a class="withdraw text" href="${url}/withdraw" data-icon="Z">Pause</a><a class="text" href="${url}" data-icon="G">Resume</a></div></div>`,
      )
      .find('#announce .withdraw')
      .on('click', function (this: HTMLAnchorElement) {
        window.lishogi.xhr.text('POST', this.href);
        $('#announce').remove();
        return false;
      });
  });

  pubsub.on('socket.in.new_notification', (e: any) => {
    $('#notify-toggle').attr('data-count', e.unread || 0);
    window.lishogi.sound.play('newPM');
  });

  const $friendsBox = $('#friend_box');
  pubsub.on('socket.in.following_onlines', (_, d) => {
    d.users = d.d;
    $friendsBox.friends('set', d);
  });
  pubsub.on('socket.in.following_enters', (_, d) => {
    $friendsBox.friends('enters', d);
  });
  pubsub.on('socket.in.following_leaves', name => {
    $friendsBox.friends('leaves', name);
  });
  pubsub.on('socket.in.following_playing', name => {
    $friendsBox.friends('playing', name);
  });
  pubsub.on('socket.in.following_stopped_playing', name => {
    $friendsBox.friends('stopped_playing', name);
  });
  pubsub.on('socket.in.following_joined_study', name => {
    $friendsBox.friends('study_join', name);
  });
  pubsub.on('socket.in.following_left_study', name => {
    $friendsBox.friends('study_leave', name);
  });

  if ('serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window) {
    const workerUrl = new URL(
      assetUrl(compiledScriptPath('service-worker'), {
        sameDomain: true,
      }),
      self.location.href,
    );
    workerUrl.searchParams.set('asset-url', document.body.getAttribute('data-asset-url')!);
    if (document.body.getAttribute('data-dev')) workerUrl.searchParams.set('dev', '1');
    const updateViaCache = document.body.getAttribute('data-dev') ? 'none' : 'all';
    navigator.serviceWorker.register(workerUrl.href, { scope: '/', updateViaCache }).then(reg => {
      const pushStorage = storage.make('push-subscribed2');
      const vapid = document.body.getAttribute('data-vapid');
      if (vapid && Notification.permission == 'granted') {
        reg.pushManager.getSubscription().then(sub => {
          const curKey = sub?.options.applicationServerKey;
          const isNewKey =
            curKey && btoa(String.fromCharCode.apply(null, new Uint8Array(curKey))) !== vapid;
          const resub =
            isNewKey || Number.parseInt(pushStorage.get() || '0', 10) + 43200000 < Date.now(); // 12 hours
          if (!sub || resub) {
            const subscribeOptions = {
              userVisibleOnly: true,
              applicationServerKey: Uint8Array.from(atob(vapid), c => c.charCodeAt(0)),
            };
            (isNewKey
              ? sub.unsubscribe().then(() => reg.pushManager.subscribe(subscribeOptions))
              : reg.pushManager.subscribe(subscribeOptions)
            ).then(
              sub =>
                fetch('/push/subscribe', {
                  method: 'POST',
                  headers: {
                    'Content-Type': 'application/json',
                  },
                  body: JSON.stringify(sub),
                }).then(res => {
                  if (res.ok && !res.redirected) pushStorage.set(`${Date.now()}`);
                  else sub.unsubscribe();
                }),
              err => {
                console.log('push subscribe failed', err.message);
                if (sub) sub.unsubscribe();
              },
            );
          }
        });
      } else {
        pushStorage.remove();
        reg.pushManager.getSubscription().then(sub => sub?.unsubscribe());
      }
    });
  }
}

export function initSocket(): void {
  if (!window.lishogi.socket) window.lishogi.socket = new StrongSocket('/socket/v4', false);
}
