/// <reference types="../types/ab" />
import * as ab from 'ab/site';

import { scrollToInnerSelector, requestIdleCallbackSafe } from 'lib';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { prefersLightThemeQuery } from 'lib/device';
import * as licon from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { eventuallySetupDefaultConnection } from 'lib/socket';
import { initMiniBoards, initMiniGames, updateMiniGame, finishMiniGame, toggleBoxInit } from 'lib/view';
import { watchers } from 'lib/view/watchers';
import { text as xhrText } from 'lib/xhr';

import { display as announceDisplay } from './announce';
import { upgradeNag } from './browserSupport';
import { addDomHandlers } from './domHandlers';
import OnlineFriends from './friends';
import powertip from './powertip';
import { updateTimeAgo, renderTimeAgo, renderLocalizedTimestamps } from './renderTimeAgo';
import serviceWorker from './serviceWorker';
import { addExceptionListeners } from './unhandledError';

export function boot() {
  addExceptionListeners();
  const setBlind = location.hash === '#blind';
  const showDebug = location.hash.startsWith('#debug');

  requestAnimationFrame(() => {
    initMiniBoards();
    initMiniGames();
    pubsub.on('content-loaded', initMiniBoards);
    pubsub.on('content-loaded', initMiniGames);
    updateTimeAgo(1000);
    pubsub.on('content-loaded', renderTimeAgo);
    renderLocalizedTimestamps();
    pubsub.on('content-loaded', toggleBoxInit);
  });
  requestIdleCallbackSafe(() => {
    const friendsEl = document.getElementById('friend_box');
    if (friendsEl) new OnlineFriends(friendsEl);

    const chatMembers = document.querySelector<HTMLElement>('.chat__members');
    if (chatMembers) watchers(chatMembers);

    $('.subnav__inner').each(function (this: HTMLElement) {
      scrollToInnerSelector(this, '.active', true);
    });

    powertip.watchMouse();

    addDomHandlers();

    toggleBoxInit();

    window.addEventListener('resize', dispatchChessgroundResize);

    ab.init();

    if (setBlind && !site.blindMode) setTimeout(() => $('#blind-mode button').trigger('click'), 1500);

    if (site.debug) site.asset.loadEsm('bits.devMode');
    if (showDebug) site.asset.loadEsm('bits.diagnosticDialog');

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
    pubsub.on('socket.in.announce', announceDisplay);
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
    const mql = prefersLightThemeQuery();
    if (typeof mql.addEventListener === 'function')
      mql.addEventListener('change', e => {
        if (document.body.dataset.theme === 'system')
          document.documentElement.className = e.matches ? 'light' : 'dark';
      });

    upgradeNag();
    mirrorCheck();
  }, 800);
}

function mirrorCheck() {
  const mirrors: string[] = ['orbitofavatars.com', 'bealive.fit'];
  if (mirrors.includes(location.host)) location.href = 'https://lichess.org' + location.pathname;
}
