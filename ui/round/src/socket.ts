import * as game from 'game';
import throttle from 'common/throttle';
import modal from 'common/modal';
import notify from 'common/notification';
import { isPlayerTurn } from 'game';
import * as xhr from './xhr';
import * as sound from './sound';
import RoundController from './ctrl';
import { Untyped } from './interfaces';
import { defined } from 'common';

export interface RoundSocket extends Untyped {
  send: SocketSend;
  handlers: Untyped;
  moreTime(): void;
  outoftime(): void;
  berserk(): void;
  sendLoading(typ: string, data?: any): void;
  receive(typ: string, data: any): boolean;
}

interface Incoming {
  t: string;
  d: any;
}

interface Handlers {
  [key: string]: (data: any) => void;
}

type Callback = (...args: any[]) => void;

function backoff(delay: number, factor: number, callback: Callback): Callback {
  let timer: number | undefined;
  let lastExec = 0;

  return function (this: any, ...args: any[]): void {
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
  };
}

export function make(send: SocketSend, ctrl: RoundController): RoundSocket {
  lichess.socket.sign(ctrl.sign);

  function reload(o: Incoming, isRetry?: boolean) {
    // avoid reload if possible!
    if (o && o.t) {
      ctrl.setLoading(false);
      handlers[o.t](o.d);
    } else
      xhr.reload(ctrl).then(data => {
        if (lichess.socket.getVersion() > data.player.version) {
          // race condition! try to reload again
          if (isRetry) lichess.reload();
          // give up and reload the page
          else reload(o, true);
        } else ctrl.reload(data);
      }, lichess.reload);
  }

  const handlers: Handlers = {
    takebackOffers(o) {
      ctrl.setLoading(false);
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      const fromOp = (ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color]);
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
        ctrl.data.correspondence.white = o.white;
        ctrl.data.correspondence.black = o.black;
        ctrl.corresClock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd(o) {
      ['white', 'black'].forEach(c => {
        if (defined(o[c])) game.setOnGame(ctrl.data, c as Color, o[c]);
      });
      ctrl.redraw();
    },
    endData: ctrl.endWithData,
    rematchOffer(by: Color) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      if ((ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color))
        notify(ctrl.noarg('yourOpponentWantsToPlayANewGameWithYou'));
      ctrl.redraw();
    },
    rematchTaken(nextId: string) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by) {
      ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
      const fromOp = (ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color);
      if (fromOp) notify(ctrl.noarg('yourOpponentOffersADraw'));
      ctrl.redraw();
    },
    berserk(color: Color) {
      ctrl.setBerserk(color);
    },
    gone: ctrl.setGone,
    goneIn: ctrl.setGone,
    checkCount(e) {
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
        !isPlayerTurn(ctrl.data)
      ) {
        ctrl.setRedirecting();
        sound.move();
        location.href = '/' + gameId;
      }
    },
    simulEnd(simul: game.Simul) {
      lichess.loadCssPath('modal');
      modal(
        $(
          '<p>Simul complete!</p><br /><br />' +
            `<a class="button" href="/simul/${simul.id}">Back to ${simul.name} simul</a>`
        )
      );
    },
  };

  lichess.pubsub.on('ab.rep', n => send('rep', { n }));

  return {
    send,
    handlers,
    moreTime: throttle(300, () => send('moretime')),
    outoftime: backoff(500, 1.1, () => send('flag', ctrl.data.game.player)),
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
    reload,
  };
}
