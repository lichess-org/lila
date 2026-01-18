import { type Simul, setOnGame, isPlayerTurn } from 'lib/game';
import { throttle } from 'lib/async';
import { reload as xhrReload } from './xhr';
import type RoundController from './ctrl';
import { defined } from 'lib';
import { domDialog } from 'lib/view';
import { pubsub } from 'lib/pubsub';
import { wsSign, wsVersion } from 'lib/socket';
import type { RoundSocketSend, EventsWithoutPayload } from './interfaces';
import { COLORS } from 'chessops';

export interface RoundSocket {
  send: RoundSocketSend;
  handlers: SocketHandlers;
  moreTime(): void;
  outoftime(): void;
  berserk(): void;
  sendLoading(typ: EventsWithoutPayload): void;
  receive(typ: string, data: any): boolean;
  reload(o?: Incoming, isRetry?: boolean): void;
}

interface Incoming {
  t: string;
  d: any;
}

type Callback = (...args: any[]) => void;

function backoff(delay: number, factor: number, callback: Callback): Callback {
  let timer: number | undefined;
  let lastExec = 0;

  return function (this: any, ...args: any[]): void {
    const self: any = this;
    const elapsed = performance.now() - lastExec;

    const exec = () => {
      timer = undefined;
      lastExec = performance.now();
      delay *= factor;
      callback.apply(self, args);
    };

    if (timer) clearTimeout(timer);

    if (elapsed > delay) exec();
    else timer = setTimeout(exec, delay - elapsed);
  };
}

export function make(send: RoundSocketSend, ctrl: RoundController): RoundSocket {
  wsSign(ctrl.sign);

  const reload = (o?: Incoming, isRetry?: boolean) => {
    // avoid reload if possible!
    if (o && o.t) {
      ctrl.setLoading(false);
      handlers[o.t]!(o.d);
    } else
      xhrReload(ctrl.data).then(data => {
        const version = wsVersion();
        if (version !== false && version > data.player.version) {
          // race condition! try to reload again
          if (isRetry) site.reload();
          // give up and reload the page
          else reload(o, true);
        } else ctrl.reload(data);
      }, site.reload);
  };

  const handlers: SocketHandlers = {
    takebackOffers(o: { white?: boolean; black?: boolean }) {
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      const fromOp = (ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color]);
      if (fromOp) ctrl.opponentRequest('takeback', i18n.site.yourOpponentProposesATakeback);
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
      COLORS.forEach(c => {
        if (defined(o[c])) setOnGame(ctrl.data, c, o[c]);
      });
      ctrl.redraw();
    },
    endData: ctrl.endWithData,
    rematchOffer(by: Color) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      if ((ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color))
        ctrl.opponentRequest('rematch', i18n.site.yourOpponentWantsToPlayANewGameWithYou);
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
        if (fromOp) ctrl.opponentRequest('draw', i18n.site.yourOpponentOffersADraw);
      }
      if (by) {
        let ply = ctrl.lastPly();
        if ((by === 'white') === (ply % 2 === 0)) ply++;
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
      ctrl.data.player.checks = ctrl.data.player.color === 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color === 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove(gameId: string) {
      if (
        ctrl.opts.userId &&
        ctrl.data.simul &&
        ctrl.opts.userId === ctrl.data.simul.hostId &&
        gameId !== ctrl.data.game.id &&
        ctrl.moveOn.get() &&
        !isPlayerTurn(ctrl.data)
      ) {
        ctrl.setRedirecting();
        site.sound.play('move');
        location.href = '/' + gameId;
      }
    },
    simulEnd(simul: Simul) {
      domDialog({
        htmlText:
          '<div><p>Simul complete!</p><br /><br />' +
          `<a class="button" href="/simul/${simul.id}">Back to ${simul.name} simul</a></div>`,
      });
    },
  };

  pubsub.on('ab.rep', n => send('rep', { n }));

  return {
    send,
    handlers,
    moreTime: throttle(300, () => send('moretime')),
    outoftime: backoff(500, 1.1, () => send('flag', ctrl.data.game.player)),
    berserk: throttle(200, () => send('berserk', undefined, { ackable: true })),
    sendLoading(typ: EventsWithoutPayload) {
      ctrl.setLoading(true);
      send(typ);
    },
    receive(typ: string, data: any): boolean {
      const handler = handlers[typ];
      if (handler) {
        handler(data);
        return true;
      }
      return false;
    },
    reload,
  };
}
