import * as licon from 'common/licon';
import { initMiniBoards, initMiniGames, updateMiniGame, finishMiniGame } from 'common/miniBoard';
import { prefersLight } from 'common/theme';
import { text as xhrText } from 'common/xhr';
import announce from './announce';
import OnlineFriends from './friends';
import powertip from './powertip';
import serviceWorker from './serviceWorker';
import { watchers } from 'common/watchers';
import { isIos } from 'common/device';
import { scrollToInnerSelector, requestIdleCallback } from 'common';
import { dispatchChessgroundResize } from 'common/resize';
import { attachDomHandlers } from './domHandlers';
import { updateTimeAgo, renderTimeAgo } from './renderTimeAgo';
import { pubsub } from 'common/pubsub';
import { toggleBoxInit } from 'common/controls';
import { addExceptionListeners } from './unhandledError';
import { eventuallySetupDefaultConnection } from 'common/socket';

export function boot() {
  addExceptionListeners();
  $('#user_tag').removeAttr('href');
  const setBlind = location.hash === '#blind';
  const showDebug = location.hash.startsWith('#debug');

  requestAnimationFrame(() => {
    initMiniBoards();
    initMiniGames();
    pubsub.on('content-loaded', initMiniBoards);
    pubsub.on('content-loaded', initMiniGames);
    updateTimeAgo(1000);
    pubsub.on('content-loaded', renderTimeAgo);
    pubsub.on('content-loaded', toggleBoxInit);
  });
  requestIdleCallback(() => {
    const friendsEl = document.getElementById('friend_box');
    if (friendsEl) new OnlineFriends(friendsEl);

    const chatMembers = document.querySelector('.chat__members') as HTMLElement | null;
    if (chatMembers) watchers(chatMembers);

    $('.subnav__inner').each(function (this: HTMLElement) {
      scrollToInnerSelector(this, '.active', true);
    });

    powertip.watchMouse();

    attachDomHandlers();

    // prevent zoom when keyboard shows on iOS
    if (isIos() && !('MSStream' in window)) {
      const el = document.querySelector('meta[name=viewport]') as HTMLElement;
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    toggleBoxInit();

    window.addEventListener('resize', dispatchChessgroundResize);

    if (setBlind && !site.blindMode) setTimeout(() => $('#blind-mode button').trigger('click'), 1500);

    if (site.debug) site.asset.loadEsm('bits.devMode');
    if (showDebug) site.asset.loadEsm('bits.diagnosticDialog');

    const pageAnnounce = document.body.getAttribute('data-announce');
    if (pageAnnounce) announce(JSON.parse(pageAnnounce));

    serviceWorker();

    console.info('Lichess is open source! See https://lichess.org/source');

    // if not already connected by a ui module, setup default connection
    eventuallySetupDefaultConnection();

    // socket default receive handlers
    pubsub.on('socket.in.redirect', (d: RedirectTo) => {
      site.unload.expected = true;
      site.redirect(d);
    });
    pubsub.on('socket.in.fen', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => updateMiniGame(el, e)),
    );
    pubsub.on('socket.in.finish', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => finishMiniGame(el, e.win)),
    );
    pubsub.on('socket.in.announce', announce);
    pubsub.on('socket.in.tournamentReminder', (data: { id: string; name: string }) => {
      if ($('#announce').length || document.body.dataset.tournamentId === data.id) return;
      const url = '/tournament/' + data.id;
      $('body').append(
        $('<div id="announce">')
          .append($(`<a data-icon="${licon.Trophy}" class="text">`).attr('href', url).text(data.name))
          .append(
            $('<div class="actions">')
              .append(
                $(`<a class="withdraw text" data-icon="${licon.Pause}">`)
                  .attr('href', url + '/withdraw')
                  .text(i18n.site.pause)
                  .on('click', function (this: HTMLAnchorElement) {
                    xhrText(this.href, { method: 'post' });
                    $('#announce').remove();
                    return false;
                  }),
              )
              .append(
                $(`<a class="text" data-icon="${licon.PlayTriangle}">`)
                  .attr('href', url)
                  .text(i18n.site.resume),
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
