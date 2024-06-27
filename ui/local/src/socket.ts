import throttle from 'common/throttle';
import { GameCtrl } from './gameCtrl';
import { ApiMove } from 'game';
import { RoundSocket } from 'round';

export function makeSocket(/*send: SocketSend, */ ctrl: GameCtrl): RoundSocket {
  const handlers: SocketHandlers = {
    move: (m: ApiMove) => {
      ctrl.round?.apiMove?.(m);
      //console.log('apiMove', m);
    },
  };
  const send = (t: string, d?: any) => {
    if (t === 'move') {
      //console.log('movin on up', t, d);
      ctrl.move(d.u);
    } else if (handlers[t]) handlers[t]?.(d);
    else console.log('no handler for', t, d);
  };
  return {
    send,
    handlers,
    moreTime: throttle(300, () => send('moretime')),
    outoftime: () => {}, //backoff(500, 1.1, () => send('flag', ctrl.data.game.player)),
    berserk: () => {},
    sendLoading(typ: string, data?: any) {
      send(typ, data);
    },
    receive: (typ: string, data: any) => {
      if (handlers[typ]) handlers[typ]?.(data);
      else console.log('no handler for', typ, data);
      return true;
    },
    reload: site.reload,
  };
}
/*

  const handlers: SocketHandlers = {
    takebackOffers(o: { white?: boolean; black?: boolean }) {
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      const fromOp = (ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color]);
      if (fromOp) ctrl.opponentRequest('takeback', 'yourOpponentProposesATakeback');
      ctrl.redraw();
    },
    move: ctrl.apiMove,
    drop: ctrl.apiMove,
    reload,
    redirect: ctrl.setRedirecting,
    clockInc(o: { color: Color; time: number }) {
      if (ctrl.clock) {
        ctrl.clock.addTime(o.color, o.time);
        ctrl.redraw();
      }
    },
    cclock(o: { white: number; black: number }) {
      if (ctrl.corresClock) {
        ctrl.data.correspondence!.white = o.white;
        ctrl.data.correspondence!.black = o.black;
        ctrl.corresClock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd(o: { white: boolean; black: boolean }) {
      (['white', 'black'] as const).forEach(c => {
        if (defined(o[c])) game.setOnGame(ctrl.data, c, o[c]);
      });
      ctrl.redraw();
    },
    endData: ctrl.endWithData,
    rematchOffer(by: Color) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      if ((ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color))
        ctrl.opponentRequest('rematch', 'yourOpponentWantsToPlayANewGameWithYou');
      ctrl.redraw();
    },
    rematchTaken(nextId: string) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by?: Color) {
      if (ctrl.isPlaying()) {
        ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
        const fromOp = (ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color);
        if (fromOp) ctrl.opponentRequest('draw', 'yourOpponentOffersADraw');
      }
      if (by) {
        let ply = ctrl.lastPly();
        if ((by == 'white') == (ply % 2 == 0)) ply++;
        ctrl.data.game.drawOffers = (ctrl.data.game.drawOffers || []).concat([ply]);
      }
      ctrl.redraw();
    },
    berserk(color: Color) {
      ctrl.setBerserk(color);
    },
    gone: ctrl.setGone,
    goneIn: ctrl.setGone,
    checkCount(e: { white: number; black: number }) {
      ctrl.data.player.checks = ctrl.data.player.color == 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color == 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove(gameId: string) {
      if (
        ctrl.opts.userId &&
        ctrl.data.simul &&
        ctrl.opts.userId == ctrl.data.simul.hostId &&
        gameId !== ctrl.data.game.id &&
        ctrl.moveOn.get() &&
        !game.isPlayerTurn(ctrl.data)
      ) {
        ctrl.setRedirecting();
        site.sound.play('move');
        location.href = '/' + gameId;
      }
    },
    simulEnd(simul: game.Simul) {
      domDialog({
        htmlText:
          '<div><p>Simul complete!</p><br /><br />' +
          `<a class="button" href="/simul/${simul.id}">Back to ${simul.name} simul</a></div>`,
      });
    },
*/
