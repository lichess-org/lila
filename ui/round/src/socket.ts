import { game } from 'game';
import { throttle } from 'common';
import * as xhr from './xhr';
import * as sound from './sound';
import RoundController from './ctrl';
import { Untyped } from './interfaces';

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

export function make(send: SocketSend, ctrl: RoundController): RoundSocket {

  function reload(o: any, isRetry?: boolean) {
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

  const handlers = {
    takebackOffers(o) {
      ctrl.setLoading(false);
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      const fromOp = ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      if (fromOp) li.desktopNotification(ctrl.trans.noarg('yourOpponentProposesATakeback'));
      ctrl.redraw();
    },
    move: ctrl.apiMove,
    drop: ctrl.apiMove,
    reload,
    redirect: ctrl.setRedirecting,
    clock(o) {
      if (ctrl.clock) {
        ctrl.clock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd(o) {
      game.setOnGame(ctrl.data, 'white', o['white']);
      game.setOnGame(ctrl.data, 'black', o['black']);
      ctrl.redraw();
    },
    // end: function(winner) { } // use endData instead
    endData(o) {
      ctrl.endWithData(o);
    },
    rematchOffer(by) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      const fromOp = ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color;
      if (fromOp) li.desktopNotification(ctrl.trans.noarg('yourOpponentWantsToPlayANewGameWithYou'));
      ctrl.redraw();
    },
    rematchTaken(nextId) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by) {
      ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
      const fromOp = ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color;
      if (fromOp) li.desktopNotification(ctrl.trans.noarg('yourOpponentOffersADraw'));
      ctrl.redraw();
    },
    berserk(color) {
      ctrl.setBerserk(color);
    },
    gone(isGone) {
      if (!ctrl.data.opponent.ai) {
        game.setIsGone(ctrl.data, ctrl.data.opponent.color, isGone);
        ctrl.redraw();
      }
    },
    checkCount(e) {
      ctrl.data.player.checks = ctrl.data.player.color == 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color == 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove(gameId) {
      if (
        ctrl.opts.userId &&
        ctrl.data.simul &&
        ctrl.opts.userId == ctrl.data.simul.hostId &&
          gameId !== ctrl.data.game.id &&
          ctrl.moveOn.get() &&
          ctrl.chessground.state.turnColor !== ctrl.chessground.state.movable.color) {
        ctrl.setRedirecting();
        sound.move();
        li.hasToReload = true;
        location.href = '/' + gameId;
      }
    }
  };

  li.pubsub.on('ab.rep', n => send('rep', { n: n }));

  return {
    send,
    handlers,
    moreTime: throttle(300, false, () => send('moretime')),
    outoftime: throttle(500, false, () => send('flag', ctrl.data.game.player)),
    berserk: throttle(200, false, () => send('berserk', null, { ackable: true })),
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
