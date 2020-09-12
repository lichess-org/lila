import { LobbyOpts } from './interfaces';
import main from './main';
import modal from 'common/modal';
import * as xhr from 'common/xhr';

export default function LichessLobby(opts: LobbyOpts) {

  opts.element = document.querySelector('.lobby__app') as HTMLElement;
  opts.pools = [ // mirrors modules/pool/src/main/PoolList.scala
    { id: "1+0", lim: 1, inc: 0, perf: "Bullet" },
    { id: "2+1", lim: 2, inc: 1, perf: "Bullet" },
    { id: "3+0", lim: 3, inc: 0, perf: "Blitz" },
    { id: "3+2", lim: 3, inc: 2, perf: "Blitz" },
    { id: "5+0", lim: 5, inc: 0, perf: "Blitz" },
    { id: "5+3", lim: 5, inc: 3, perf: "Blitz" },
    { id: "10+0", lim: 10, inc: 0, perf: "Rapid" },
    { id: "10+5", lim: 10, inc: 5, perf: "Rapid" },
    { id: "15+10", lim: 15, inc: 10, perf: "Rapid" },
    { id: "30+0", lim: 30, inc: 0, perf: "Classical" },
    { id: "30+20", lim: 30, inc: 20, perf: "Classical" }
  ];
  const li = window.lichess,
    nbRoundSpread = spreadNumber(
      document.querySelector('#nb_games_in_play > strong') as HTMLElement,
      8),
    nbUserSpread = spreadNumber(
      document.querySelector('#nb_connected_players > strong') as HTMLElement,
      10),
    getParameterByName = (name: string) => {
      const match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
      return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    };
  let lobby: any;
  li.socket = new li.StrongSocket(
    '/lobby/socket/v5',
    false, {
    receive(t: string, d: any) { lobby.socketReceive(t, d) },
    events: {
      n(_: string, msg: any) {
        nbUserSpread(msg.d);
        setTimeout(() => nbRoundSpread(msg.r), li.socket.pingInterval() / 2);
      },
      reload_timeline() {
        xhr.text('/timeline').then(html => {
          $('.timeline').html(html);
          li.pubsub.emit('content_loaded');
        });
      },
      featured(o: { html: string }) {
        $('.lobby__tv').html(o.html);
        li.pubsub.emit('content_loaded');
      },
      redirect(e: RedirectTo) {
        lobby.leavePool();
        lobby.setRedirecting();
        li.redirect(e);
      },
      fen(e: any) {
        lobby.gameActivity(e.id);
      }
    }
  });
  li.StrongSocket.firstConnect.then(() => {
    const gameId = getParameterByName('hook_like');
    if (!gameId) return;
    xhr.text(
      `/setup/hook/${li.sri}/like/${gameId}?rr=${lobby.setup.ratingRange() || ''}`,
      { method: 'post' });
    lobby.setTab('real_time');
    history.replaceState(null, '', '/');
  });

  opts.blindMode = $('body').hasClass('blind-mode');
  opts.trans = li.trans(opts.i18n);
  opts.socketSend = li.socket.send;
  lobby = main(opts);

  const $startButtons = $('.lobby__start'),
    clickEvent = opts.blindMode ? 'click' : 'mousedown';

  $startButtons.find('a:not(.disabled)').on(clickEvent, function(this: HTMLAnchorElement) {
    $(this).addClass('active').siblings().removeClass('active');
    li.loadCssPath('lobby.setup');
    lobby.leavePool();
    xhr.text(this.href)
      .then(html => {
        lobby.setup.prepareForm(modal(html, 'game-setup', () => {
          $startButtons.find('.active').removeClass('active');
        }));
        li.pubsub.emit('content_loaded');
      })
      .catch(li.reload);
    return false;
  }).on('click', () => false);

  if (['#ai', '#friend', '#hook'].includes(location.hash)) {
    $startButtons
      .find('.config_' + location.hash.replace('#', ''))
      .each(function(this: HTMLElement) {
        $(this).attr("href", $(this).attr("href") + location.search);
      }).trigger(clickEvent);

    if (location.hash === '#hook') {
      if (/time=realTime/.test(location.search))
        lobby.setTab('real_time');
      else if (/time=correspondence/.test(location.search))
        lobby.setTab('seeks');
    }

    history.replaceState(null, '', '/');
  }
}

function spreadNumber(el: HTMLElement, nbSteps: number) {
  let previous: number, displayed: string;
  const display = (prev: number, cur: number, it: number) => {
    const val = window.lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  let timeouts: number[] = [];
  return (nb: number, overrideNbSteps?: number) => {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    let prev = previous === 0 ? 0 : (previous || nb);
    previous = nb;
    let interv = Math.abs(window.lichess.socket.pingInterval() / nbSteps);
    for (let i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(() => display(prev, nb, i), Math.round(i * interv)));
  };
}
