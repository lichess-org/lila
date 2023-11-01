import * as licon from 'common/licon';
import * as miniBoard from 'common/mini-board';
import * as miniGame from './component/mini-game';
import * as timeago from './component/timeago';
import * as xhr from 'common/xhr';
import announce from './component/announce';
import agreement from './component/agreement';
import exportLichessGlobals from './site.lichess.globals';
import info from './component/info';
import OnlineFriends from './component/friends';
import powertip from './component/powertip';
import pubsub from './component/pubsub';
import serviceWorker from './component/serviceWorker';
import StrongSocket from './component/socket';
import topBar from './component/top-bar';
import watchers from './component/watchers';
import { reload } from './component/reload';
import { requestIdleCallback } from './component/functions';
import { userComplete } from './component/assets';
import { siteTrans } from './component/trans';
import { isIOS } from 'common/device';
import { scrollToInnerSelector } from 'common';

window.$as = <T>(cashOrHtml: Cash | string) =>
  (typeof cashOrHtml === 'string' ? $(cashOrHtml) : cashOrHtml)[0] as T;
exportLichessGlobals();
lichess.info = info;

lichess.load.then(() => {
  $('#user_tag').removeAttr('href');

  requestAnimationFrame(() => {
    miniBoard.initAll();
    miniGame.initAll();
    pubsub.on('content-loaded', miniBoard.initAll);
    pubsub.on('content-loaded', miniGame.initAll);
    timeago.updateRegularly(1000);
    pubsub.on('content-loaded', timeago.findAndRender);
  });
  requestIdleCallback(() => {
    const friendsEl = document.getElementById('friend_box');
    if (friendsEl) new OnlineFriends(friendsEl);

    const chatMembers = document.querySelector('.chat__members') as HTMLElement | null;
    if (chatMembers) watchers(chatMembers);

    $('.subnav__inner').each(function (this: HTMLElement) {
      scrollToInnerSelector(this, '.active', true);
    });
    $('#main-wrap')
      .on('click', '.autoselect', function (this: HTMLInputElement) {
        this.select();
      })
      .on('click', 'button.copy', function (this: HTMLElement) {
        const showCheckmark = () => $(this).attr('data-icon', licon.Checkmark);
        $('#' + this.dataset.rel).each(function (this: HTMLInputElement) {
          try {
            navigator.clipboard.writeText(this.value).then(showCheckmark);
          } catch (e) {
            console.error(e);
          }
        });
        return false;
      });

    $('body').on('click', 'a.relation-button', function (this: HTMLAnchorElement) {
      const $a = $(this).addClass('processing').css('opacity', 0.3);
      xhr.text(this.href, { method: 'post' }).then(html => {
        if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
      return false;
    });

    $('.mselect .button').on('click', function (this: HTMLElement) {
      const $p = $(this).parent();
      $p.toggleClass('shown');
      requestIdleCallback(() => {
        const handler = (e: Event) => {
          if ($p[0]!.contains(e.target as HTMLElement)) return;
          $p.removeClass('shown');
          $('html').off('click', handler);
        };
        $('html').on('click', handler);
      }, 200);
    });

    powertip.watchMouse();

    setTimeout(() => {
      if (!lichess.socket) lichess.socket = new StrongSocket('/socket/v5', false);
    }, 300);

    topBar();

    window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));

    $('.user-autocomplete').each(function (this: HTMLInputElement) {
      const focus = !!this.autofocus;
      const start = () =>
        userComplete({
          input: this,
          friend: !!this.dataset.friend,
          tag: this.dataset.tag as any,
          focus,
        });

      if (focus) start();
      else $(this).one('focus', start);
    });

    $('input.confirm, button.confirm').on('click', function (this: HTMLElement) {
      return confirm(this.title || 'Confirm this action?');
    });

    $('#main-wrap').on('click', 'a.bookmark', function (this: HTMLAnchorElement) {
      const t = $(this).toggleClass('bookmarked');
      xhr.text(this.href, { method: 'post' });
      const count = (parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
      t.find('span').html('' + (count > 0 ? count : ''));
      return false;
    });

    /* Edge randomly fails to rasterize SVG on page load
     * A different SVG must be loaded so a new image can be rasterized */
    if (navigator.userAgent.includes('Edge/'))
      setTimeout(() => {
        const sprite = document.getElementById('piece-sprite') as HTMLLinkElement;
        sprite.href = sprite.href.replace('.css', '.external.css');
      }, 1000);

    // prevent zoom when keyboard shows on iOS
    if (isIOS() && !('MSStream' in window)) {
      const el = document.querySelector('meta[name=viewport]') as HTMLElement;
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    if (location.hash === '#blind' && !lichess.blindMode)
      xhr
        .text('/toggle-blind-mode', {
          method: 'post',
          body: xhr.form({
            enable: 1,
            redirect: '/',
          }),
        })
        .then(reload);

    const pageAnnounce = document.body.getAttribute('data-announce');
    if (pageAnnounce) announce(JSON.parse(pageAnnounce));

    agreement();

    serviceWorker();

    // socket default receive handlers
    pubsub.on('socket.in.redirect', (d: RedirectTo) => {
      lichess.unload.expected = true;
      lichess.redirect(d);
    });
    pubsub.on('socket.in.fen', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => miniGame.update(el, e)),
    );
    pubsub.on('socket.in.finish', e =>
      document
        .querySelectorAll('.mini-game-' + e.id)
        .forEach((el: HTMLElement) => miniGame.finish(el, e.win)),
    );
    pubsub.on('socket.in.announce', announce);
    pubsub.on('socket.in.tournamentReminder', (data: { id: string; name: string }) => {
      if ($('#announce').length || document.body.dataset.tournamentId == data.id) return;
      const url = '/tournament/' + data.id;
      $('body').append(
        $('<div id="announce">')
          .append($(`<a data-icon="${licon.Trophy}" class="text">`).attr('href', url).text(data.name))
          .append(
            $('<div class="actions">')
              .append(
                $(`<a class="withdraw text" data-icon="${licon.Pause}">`)
                  .attr('href', url + '/withdraw')
                  .text(siteTrans('pause'))
                  .on('click', function (this: HTMLAnchorElement) {
                    xhr.text(this.href, { method: 'post' });
                    $('#announce').remove();
                    return false;
                  }),
              )
              .append(
                $(`<a class="text" data-icon="${licon.PlayTriangle}">`)
                  .attr('href', url)
                  .text(siteTrans('resume')),
              ),
          ),
      );
    });
  }, 800);
});
