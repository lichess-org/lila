import * as licon from 'lib/licon';
import {
  initMiniBoards,
  initMiniGames,
  updateMiniGame,
  finishMiniGame,
  toggleBoxInit,
  alert,
} from 'lib/view';
import { text as xhrText } from 'lib/xhr';
import { display as announceDisplay } from './announce';
import OnlineFriends from './friends';
import powertip from './powertip';
import serviceWorker from './serviceWorker';
import { watchers } from 'lib/view/watchers';
import { isIos, isWebkit, prefersLightThemeQuery } from 'lib/device';
import { scrollToInnerSelector, requestIdleCallback } from 'lib';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import { addDomHandlers } from './domHandlers';
import { updateTimeAgo, renderTimeAgo, renderLocalizedTimestamps } from './renderTimeAgo';
import { pubsub } from 'lib/pubsub';
import { once } from 'lib/storage';
import { addExceptionListeners } from './unhandledError';
import { eventuallySetupDefaultConnection } from 'lib/socket';

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
    renderLocalizedTimestamps();
    pubsub.on('content-loaded', renderLocalizedTimestamps);
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

    addDomHandlers();

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

    serviceWorker();

    console.info('Lichess is open source! See https://lichess.org/source');

    // if not already connected by a ui module, setup default connection
    eventuallySetupDefaultConnection();

    if (isUnsupportedBrowser() && once('upgrade.nag', { days: 14 })) {
      pubsub
        .after('polyfill.dialog')
        .then(() => alert('Your browser is out of date.\nLichess may not work properly.'));
    }

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

    mirrorCheck();
  }, 800);
}

const isUnsupportedBrowser = () => isWebkit({ below: '15.4' });

function mirrorCheck() {
  const mirrors: string[] = [
    'lichess.dscs2009.com',
    'joystickcaravan.org',
    'gamelorecollective.com',
    'joystick-astral.com',
    'orbitofavatars.com',
  ];
  if (mirrors.includes(location.host)) location.href = 'https://lichess.org' + location.pathname;
}
