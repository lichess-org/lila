import * as licon from 'common/licon';
import * as miniBoard from 'common/miniBoard';
import { prefersLight } from 'common/theme';
import * as miniGame from './miniGame';
import * as timeago from './timeago';
import * as xhr from 'common/xhr';
import announce from './announce';
import OnlineFriends from './friends';
import powertip from './powertip';
import pubsub from './pubsub';
import serviceWorker from './serviceWorker';
import StrongSocket from './socket';
import topBar from './topBar';
import watchers from './watchers';
import { siteTrans } from './trans';
import { isIOS } from 'common/device';
import { scrollToInnerSelector, requestIdleCallback } from 'common';
import { dispatchChessgroundResize } from 'common/resize';

export function boot() {
  $('#user_tag').removeAttr('href');
  const setBlind = location.hash === '#blind';
  const showDebug = location.hash.startsWith('#debug');

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
    $('#main-wrap').on('click', '.copy-me__button', function (this: HTMLElement) {
      const showCheckmark = () => {
        $(this).attr('data-icon', licon.Checkmark).removeClass('button-metal');
        setTimeout(() => $(this).attr('data-icon', licon.Clipboard).addClass('button-metal'), 1000);
      };
      $(this.parentElement!.firstElementChild!).each(function (this: any) {
        try {
          navigator.clipboard.writeText(this.value || this.href).then(showCheckmark);
        } catch (e) {
          console.error(e);
        }
      });
      return false;
    });

    $('body').on('click', '.relation-button', function (this: HTMLAnchorElement) {
      const $a = $(this).addClass('processing').css('opacity', 0.3);
      xhr.text(this.href, { method: 'post' }).then(html => {
        if ($a.hasClass('aclose')) $a.hide();
        else if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
      return false;
    });

    powertip.watchMouse();

    setTimeout(() => {
      if (!site.socket) site.socket = new StrongSocket('/socket/v5', false);
    }, 300);

    topBar();

    window.addEventListener('resize', dispatchChessgroundResize);

    $('.user-autocomplete').each(function (this: HTMLInputElement) {
      const focus = !!this.autofocus;
      const start = () =>
        site.asset.userComplete({
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

    $('.toggle-box--toggle').each(function (this: HTMLFieldSetElement) {
      const toggle = () => this.classList.toggle('toggle-box--toggle-off');
      $(this)
        .children('legend')
        .on('click', toggle)
        .on('keypress', e => e.key == 'Enter' && toggle());
    });

    if (setBlind && !site.blindMode) setTimeout(() => $('#blind-mode button').trigger('click'), 1500);

    if (site.debug) site.asset.loadEsm('bits.devMode');
    if (showDebug) site.asset.loadEsm('bits.diagnosticDialog');

    const pageAnnounce = document.body.getAttribute('data-announce');
    if (pageAnnounce) announce(JSON.parse(pageAnnounce));

    serviceWorker();

    console.info('Lichess is open source! See https://lichess.org/source');

    // socket default receive handlers
    pubsub.on('socket.in.redirect', (d: RedirectTo) => {
      site.unload.expected = true;
      site.redirect(d);
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
    prefersLight().addEventListener('change', e => {
      if (document.body.dataset.theme === 'system')
        document.documentElement.className = e.matches ? 'light' : 'dark';
    });
  }, 800);
}
