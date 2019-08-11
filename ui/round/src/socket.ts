import * as game from 'game';
import throttle from 'common/throttle';
import notify from 'common/notification';
import * as xhr from './xhr';
import * as sound from './sound';
import RoundController from './ctrl';
import { Untyped, ApiEnd } from './interfaces';
import { incSimulToMove } from './simulStanding';

const li = window.lidraughts;

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
      const fromOp = d.opponent.offeringRematch = by === d.opponent.color;
      if (fromOp) notify(ctrl.noarg('yourOpponentWantsToPlayANewGameWithYou'));
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
    kingMoves(e) {
      if (d.pref.showKingMoves) {
        ctrl.draughtsground.setKingMoves({ 
          white: { count: e.white, key: e.whiteKing },
          black: { count: e.black, key: e.blackKing }
        });
        ctrl.redraw();
      }
    },
    simulPlayerMove(gameId: string) {
      if (ctrl.opts.userId &&
        d.simul &&
        ctrl.opts.userId == d.simul.hostId) {
        incSimulToMove(ctrl.trans);
        if (gameId !== d.game.id &&
          ctrl.moveOn.get() &&
          ctrl.draughtsground.state.turnColor !== ctrl.draughtsground.state.movable.color) {
            ctrl.setRedirecting();
            sound.move();
            li.hasToReload = true;
            location.href = '/' + gameId;
        } else {
          $('#others_' + gameId).toggleClass('my_turn');
          $('#others_' + gameId + ':not(.game-timeout) .indicator').text(ctrl.trans('yourTurn'));
        }
      }
    },
    simulEnd(simul: game.Simul) {
      li.loadCssPath('modal');
      $.modal($(
        '<p>' + ctrl.trans('xComplete', simul.name) + '</p><br /><br />' +
        '<a class="button" href="/simul/' + simul.id + '">' + ctrl.trans('backToSimul') + '</a>'
      ));
    }
  };

  li.pubsub.on('ab.rep', n => send('rep', { n: n }));

  return {
    send,
    handlers,
    moreTime: throttle(300, () => send('moretime')),
    outoftime: throttle(500, () => send('flag', d.game.player)),
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
    }
  };

}
