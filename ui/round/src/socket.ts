import * as game from 'game';
import throttle from 'common/throttle';
import notify from 'common/notification';
import { isPlayerTurn } from 'game';
import * as xhr from './xhr';
import * as sound from './sound';
import RoundController from './ctrl';
import { Untyped, ApiEnd } from './interfaces';

const li = window.lichess;

export interface RoundSocket extends Untyped {
  send: SocketSend;
  handlers: Untyped;
  moreTime(): void;
  outoftime(): void;
  berserk(): void;
  sendLoading(typ: string, data?: any): void
    receive(typ: string, data: any): boolean;
}

interface Incoming {
  t: string;
  d: any;
}

interface Handlers {
  [key: string]: (data: any) => void;
}

function backoff(delay: number, factor: number, callback: (...args: any[]) => void): (...args:any[]) => void {
  let timer: number | undefined;
  let lastExec = 0;

  return function(this: any, ...args: any[]): void {
    const self: any = this;
    const elapsed = performance.now() - lastExec;

    function exec() {
      timer = undefined;
      lastExec = performance.now();
      delay *= factor;
      callback.apply(self, args);
    }

    if (timer) clearTimeout(timer);

    if (elapsed > delay) exec();
    else timer = setTimeout(exec, delay - elapsed);
  }
}

export function make(send: SocketSend, ctrl: RoundController): RoundSocket {

  function reload(o: Incoming, isRetry?: boolean) {
    // avoid reload if possible!
    if (o && o.t) {
      ctrl.setLoading(false);
      handlers[o.t](o.d);
    }
    else xhr.reload(ctrl).then(data => {
      if (li.socket.getVersion() > data.player.version) {
        // race condition! try to reload again
        if (isRetry) li.reload(); // give up and reload the page
        else reload(o, true);
      }
      else ctrl.reload(data);
    });
  };

  const d = ctrl.data;

  const handlers: Handlers = {
    takebackOffers(o) {
      ctrl.setLoading(false);
      d.player.proposingTakeback = o[d.player.color];
      const fromOp = d.opponent.proposingTakeback = o[d.opponent.color];
      if (fromOp) notify(ctrl.noarg('yourOpponentProposesATakeback'));
      ctrl.redraw();
    },
    move: ctrl.apiMove,
    drop: ctrl.apiMove,
    reload,
    redirect: ctrl.setRedirecting,
    clockInc(o) {
      if (ctrl.clock) {
        ctrl.clock.addTime(o.color, o.time);
        ctrl.redraw();
      }
    },
    cclock(o) {
      if (ctrl.corresClock) {
        d.correspondence.white = o.white;
        d.correspondence.black = o.black;
        ctrl.corresClock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd(o) {
      game.setOnGame(d, 'white', o['white']);
      game.setOnGame(d, 'black', o['black']);
      ctrl.redraw();
    },
    endData(o: ApiEnd) {
      ctrl.endWithData(o);
    },
    rematchOffer(by: Color) {
      d.player.offeringRematch = by === d.player.color;
      if (d.opponent.offeringRematch = by === d.opponent.color)
        notify(ctrl.noarg('yourOpponentWantsToPlayANewGameWithYou'));
      ctrl.redraw();
    },
    rematchTaken(nextId: string) {
      d.game.rematch = nextId;
      if (!d.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by) {
      d.player.offeringDraw = by === d.player.color;
      const fromOp = d.opponent.offeringDraw = by === d.opponent.color;
      if (fromOp) notify(ctrl.noarg('yourOpponentOffersADraw'));
      ctrl.redraw();
    },
    berserk(color: Color) {
      ctrl.setBerserk(color);
    },
    gone(isGone) {
      if (!d.opponent.ai) {
        game.setIsGone(d, d.opponent.color, isGone);
        ctrl.redraw();
      }
    },
    checkCount(e) {
      d.player.checks = d.player.color == 'white' ? e.white : e.black;
      d.opponent.checks = d.opponent.color == 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove(gameId: string) {
      if (
        ctrl.opts.userId &&
        d.simul &&
        ctrl.opts.userId == d.simul.hostId &&
        gameId !== d.game.id &&
        ctrl.moveOn.get() &&
        !isPlayerTurn(ctrl.data)) {
        ctrl.setRedirecting();
        sound.move();
        li.hasToReload = true;
        location.href = '/' + gameId;
      }
    },
    simulEnd(simul: game.Simul) {
      li.loadCssPath('modal');
      $.modal($(
        '<p>Simul complete!</p><br /><br />' +
        '<a class="button" href="/simul/' + simul.id + '">Back to ' + simul.name + ' simul</a>'
      ));
    }
  };

  li.pubsub.on('ab.rep', n => send('rep', { n: n }));

  return {
    send,
    handlers,
    moreTime: throttle(300, () => send('moretime')),
    outoftime: backoff(500, 1.1, () => send('flag', d.game.player)),
    berserk: throttle(200, () => send('berserk', null, { ackable: true })),
    sendLoading(typ: string, data?: any) {
      ctrl.setLoading(true);
      send(typ, data);
    },
    receive(typ: string, data: any): boolean {
      if (handlers[typ]) {
        handlers[typ](data);
        return true;
      }
      return false;
    },
    reload
  };
}
