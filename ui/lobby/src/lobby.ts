import * as xhr from 'lib/xhr';
import main from './main';
import type { LobbyOpts } from './interfaces';
import { wsConnect, wsPingInterval } from 'lib/socket';
import { pubsub } from 'lib/pubsub';

export function initModule(opts: LobbyOpts) {
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

  opts.socketSend = wsConnect('/lobby/socket/v5', false, {
    options: { reloadOnResume: true },
    receive: (t: string, d: any) => lobbyCtrl.socket.receive(t, d),
    events: {
      n(_: string, msg: any) {
        lobbyCtrl.spreadPlayersNumber && lobbyCtrl.spreadPlayersNumber(msg.d);
        setTimeout(
          () => lobbyCtrl.spreadGamesNumber && lobbyCtrl.spreadGamesNumber(msg.r),
          wsPingInterval() / 2,
        );
      },
      reload_timeline() {
        xhr.text('/timeline').then(html => {
          $('.timeline').html(html);
          pubsub.emit('content-loaded');
        });
      },
      featured(o: { html: string }) {
        $('.lobby__tv').html(o.html);
        pubsub.emit('content-loaded');
      },
      redirect(e: RedirectTo) {
        lobbyCtrl.setRedirecting();
        lobbyCtrl.leavePool();
        site.redirect(e, true);
        return true;
      },
      fen(e: any) {
        lobbyCtrl.gameActivity(e.id);
      },
    },
  }).send;
  pubsub.after('socket.hasConnected').then(() => {
    const gameId = new URLSearchParams(location.search).get('hook_like');
    if (!gameId) return;
    const { ratingMin, ratingMax } = lobbyCtrl.setupCtrl.makeSetupStore('hook')();
    xhr.text(
      xhr.url(`/setup/hook/${site.sri}/like/${gameId}`, { deltaMin: ratingMin, deltaMax: ratingMax }),
      {
        method: 'post',
      },
    );
    lobbyCtrl.setTab('real_time');
    lobbyCtrl.redraw();
    history.replaceState(null, '', '/');
  });

  const lobbyCtrl = main(opts);
}
