import * as xhr from 'common/xhr';
import exportLichessGlobals from "./site.lichess.globals";
import StrongSocket from "./component/socket";
import { reload } from "./component/reload";
import announce from './component/announce';
import moduleLaunchers from "./component/module-launchers";
import pubsub from "./component/pubsub";
import miniBoard from "./component/mini-board";
import miniGame from "./component/mini-game";
import { requestIdleCallback } from "./component/functions";
import powertip from "./component/powertip";
import timeago from "./component/timeago";
import topBar from "./component/top-bar";
import userAutocomplete from "./component/user-autocomplete";
import loadInfiniteScroll from "./component/infinite-scroll";
import { storage } from "./component/storage";
import { assetUrl } from "./component/assets";
import serviceWorker from "./component/service-worker";
import loadClockWidget from "./component/clock-widget";
import info from "./component/info";
import OnlineFriends from "./component/friends";
import watchers from "./component/watchers";

exportLichessGlobals();
const li = window.lichess;
li.info = info;

loadClockWidget();

li.load.then(() => {

  moduleLaunchers();

  requestIdleCallback(() => {

    const friendsEl = document.getElementById('friend_box');
    if (friendsEl) new OnlineFriends(friendsEl);

    $('#main-wrap')
      .on('click', '.autoselect', function(this: HTMLElement) {
        $(this).select();
      })
      .on('click', 'button.copy', function(this: HTMLElement) {
        $('#' + $(this).data('rel')).select();
        document.execCommand('copy');
        $(this).attr('data-icon', 'E');
      });

    $('body').on('click', 'a.relation-button', function(this: HTMLAnchorElement) {
      const $a = $(this).addClass('processing').css('opacity', 0.3);
      xhr.text(this.href, { method: 'post' }).then(html => {
        if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
      return false;
    });

    $('.mselect .button').on('click', function(this: HTMLElement) {
      const $p = $(this).parent();
      $p.toggleClass('shown');
      requestIdleCallback(() => {
        const handler = (e: Event) => {
          if ($p[0].contains(e.target as HTMLElement)) return;
          $p.removeClass('shown');
          $('html').off('click', handler);
        };
        $('html').on('click', handler);
      });
    });

    powertip.watchMouse();

    timeago.updateRegularly(1000);
    pubsub.on('content_loaded', timeago.findAndRender);

    setTimeout(() => {
      if (!li.socket)
        li.socket = new StrongSocket("/socket/v5", false);
    }, 300);

    topBar();

    window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));

    $('.user-autocomplete').each(function(this: HTMLElement) {
      const opts = {
        focus: true,
        friend: $(this).data('friend'),
        tag: $(this).data('tag')
      } as UserAutocompleteOpts;
      if ($(this).attr('autofocus')) userAutocomplete($(this), opts);
      else $(this).one('focus', function(this: HTMLElement) {
        userAutocomplete($(this), opts);
      });
    });

    loadInfiniteScroll('.infinitescroll');

    $('a.delete, input.delete').click(() => confirm('Delete?'));
    $('input.confirm, button.confirm').click(function(this: HTMLElement) {
      return confirm($(this).attr('title') || 'Confirm this action?');
    });

    $('#main-wrap').on('click', 'a.bookmark', function(this: HTMLAnchorElement) {
      const t = $(this).toggleClass('bookmarked');
      xhr.text(this.href, { method: 'post' });
      const count = (parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
      t.find('span').html('' + (count > 0 ? count : ''));
      return false;
    });

    // still bind esc even in form fields
    window.Mousetrap.prototype.stopCallback = (_: any, el: HTMLElement, combo: string) =>
      combo != 'esc' && (
        el.isContentEditable || el.tagName == 'INPUT' || el.tagName == 'SELECT' || el.tagName == 'TEXTAREA'
      );
    window.Mousetrap.bind('esc', () => {
      const $oc = $('#modal-wrap .close');
      if ($oc.length) $oc.trigger('click');
      else {
        const $input = $(':focus');
        if ($input.length) $input.trigger('blur');
      }
      return false;
    });

    if (!storage.get('grid')) setTimeout(() => {
      if (getComputedStyle(document.body).getPropertyValue('--grid'))
        storage.set('grid', '1');
      else
        xhr.text(assetUrl('oops/browser.html')).then(html => $('body').prepend(html))
    }, 3000);

    /* A disgusting hack for a disgusting browser
     * Edge randomly fails to rasterize SVG on page load
     * A different SVG must be loaded so a new image can be rasterized */
    if (navigator.userAgent.includes('Edge/')) setTimeout(() => {
      const sprite = $('#piece-sprite');
      sprite.attr('href', sprite.attr('href').replace('.css', '.external.css'));
    }, 1000);

    // prevent zoom when keyboard shows on iOS
    if (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) {
      const el = document.querySelector('meta[name=viewport]') as HTMLElement;
      el.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
    }

    miniBoard.initAll();
    miniGame.initAll();
    pubsub.on('content_loaded', miniBoard.initAll);
    pubsub.on('content_loaded', miniGame.initAll);

    const chatMembers = document.querySelector('.chat__members') as HTMLElement | null;
    if (chatMembers) watchers(chatMembers);

    if (location.hash === '#blind' && !$('body').hasClass('blind-mode'))
      xhr.text('/toggle-blind-mode', {
        method: 'post',
        body: xhr.form({
          enable: 1,
          redirect: '/'
        })
      }).then(reload);

    serviceWorker();

    // socket default receive handlers
    pubsub.on('socket.in.redirect', (d: RedirectTo) => {
      li.unload.expected = true;
      li.redirect(d);
    });
    pubsub.on('socket.in.fen', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => miniGame.update(el, e))
    );
    pubsub.on('socket.in.finish', e =>
      document.querySelectorAll('.mini-game-' + e.id).forEach((el: HTMLElement) => miniGame.finish(el, e.win))
    );
    pubsub.on('socket.in.announce', announce);
    pubsub.on('socket.in.tournamentReminder', (data: { id: string, name: string }) => {
      if ($('#announce').length || $('body').data("tournament-id") == data.id) return;
      const url = '/tournament/' + data.id;
      $('body').append(
        '<div id="announce">' +
        '<a data-icon="g" class="text" href="' + url + '">' + data.name + '</a>' +
        '<div class="actions">' +
        '<a class="withdraw text" href="' + url + '/withdraw" data-icon="Z">Pause</a>' +
        '<a class="text" href="' + url + '" data-icon="G">Resume</a>' +
        '</div></div>'
      ).find('#announce .withdraw').click(function(this: HTMLAnchorElement) {
        xhr.text(this.href, { method: 'post' });
        $('#announce').remove();
        return false;
      });
    });
  });
});
