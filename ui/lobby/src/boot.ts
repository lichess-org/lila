import * as xhr from 'common/xhr';
import main from './main';
import { LobbyOpts } from './interfaces';
import { numberFormat } from 'common/number';

export default function LichessLobby(opts: LobbyOpts) {
  opts.appElement = document.querySelector('.lobby__app') as HTMLElement;
  opts.tableElement = document.querySelector('.lobby__table') as HTMLElement;
  opts.pools = [
    // mirrors modules/pool/src/main/PoolList.scala
    { id: '1+0', lim: 1, inc: 0, perf: 'Bullet' },
    { id: '2+1', lim: 2, inc: 1, perf: 'Bullet' },
    { id: '3+0', lim: 3, inc: 0, perf: 'Blitz' },
    { id: '3+2', lim: 3, inc: 2, perf: 'Blitz' },
    { id: '5+0', lim: 5, inc: 0, perf: 'Blitz' },
    { id: '5+3', lim: 5, inc: 3, perf: 'Blitz' },
    { id: '10+0', lim: 10, inc: 0, perf: 'Rapid' },
    { id: '10+5', lim: 10, inc: 5, perf: 'Rapid' },
    { id: '15+10', lim: 15, inc: 10, perf: 'Rapid' },
    { id: '30+0', lim: 30, inc: 0, perf: 'Classical' },
    { id: '30+20', lim: 30, inc: 20, perf: 'Classical' },
  ];
  opts.blindMode = $('body').hasClass('blind-mode');
  opts.trans = lichess.trans(opts.i18n);

  const nbRoundSpread = spreadNumber('#nb_games_in_play > strong', 8),
    nbUserSpread = spreadNumber('#nb_connected_players > strong', 10),
    getParameterByName = (name: string) => {
      const match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
      return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    };
  lichess.socket = new lichess.StrongSocket('/lobby/socket/v5', false, {
    receive(t: string, d: any) {
      lobby.socketReceive(t, d);
    },
    events: {
      n(_: string, msg: any) {
        nbUserSpread(msg.d);
        setTimeout(() => nbRoundSpread(msg.r), lichess.socket.pingInterval() / 2);
      },
      reload_timeline() {
        xhr.text('/timeline').then(html => {
          $('.timeline').html(html);
          lichess.contentLoaded();
        });
      },
      featured(o: { html: string }) {
        $('.lobby__tv').html(o.html);
        lichess.contentLoaded();
      },
      redirect(e: RedirectTo) {
        lobby.leavePool();
        lobby.setRedirecting();
        lichess.redirect(e);
      },
      fen(e: any) {
        lobby.gameActivity(e.id);
      },
    },
  });
  lichess.StrongSocket.firstConnect.then(() => {
    const gameId = getParameterByName('hook_like');
    if (!gameId) return;
    const { ratingMin, ratingMax } = lobby.setupCtrl.makeSetupStore('hook')();
    xhr.text(xhr.url(`/setup/hook/${lichess.sri}/like/${gameId}`, { deltaMin: ratingMin, deltaMax: ratingMax }), {
      method: 'post',
    });
    lobby.setTab('real_time');
    history.replaceState(null, '', '/');
  });

  opts.socketSend = lichess.socket.send;
  const lobby = main(opts);

  suggestBgSwitch();
}

function suggestBgSwitch() {
  const m = window.matchMedia('(prefers-color-scheme: dark)');
  if (m.media == 'not all') return;
  const current = document.body.getAttribute('data-theme');
  if (m.matches == (current == 'dark')) return;

  let dasher: Promise<any>;
  const getDasher = (): Promise<any> => {
    dasher =
      dasher ||
      lichess.loadModule('dasher').then(() => window.LichessDasher(document.createElement('div'), { playing: false }));
    return dasher;
  };

  $('.bg-switch')
    .addClass('active')
    .on('click', () =>
      getDasher().then(dasher =>
        dasher.subs.background.set(document.body.classList.contains('dark') ? 'light' : 'dark')
      )
    );
}

function spreadNumber(selector: string, nbSteps: number) {
  let el = document.querySelector(selector) as HTMLElement;
  let previous = parseInt(el.getAttribute('data-count')!);
  const display = (prev: number, cur: number, it: number) => {
    el.textContent = numberFormat(Math.round((prev * (nbSteps - 1 - it) + cur * (it + 1)) / nbSteps));
  };
  let timeouts: number[] = [];
  return (nb: number, overrideNbSteps?: number) => {
    // find the element again since snabbdom will change it on its first draw
    el = document.querySelector(selector) as HTMLElement;
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    const interv = Math.abs(lichess.socket.pingInterval() / nbSteps);
    const prev = previous || nb;
    previous = nb;
    for (let i = 0; i < nbSteps; i++) timeouts.push(setTimeout(() => display(prev, nb, i), Math.round(i * interv)));
  };
}
