import { game } from 'game';
import { throttle } from 'common';
import * as xhr from './xhr';
import * as sound from './sound';
import RoundController from './ctrl';

const li = window.lichess;

interface Handlers {
  [key: string]: any; // #TODO
}

export default class RoundSocket {
  
  constructor(public send: SocketSend, public ctrl: RoundController) {

    li.pubsub.on('ab.rep', n => send('rep', { n: n }));
  }

  receive = (typ: string, data: any): boolean => {
    if (this.handlers[typ]) {
      this.handlers[typ](data);
      return true;
    }
    return false;
  };

  moreTime: () => void = throttle(300, false, () => this.send('moretime'));

  outoftime: () => void = throttle(500, false, () => this.send('flag', this.ctrl.data.game.player));

  berserk: () => void = throttle(200, false, () => this.send('berserk', null, { ackable: true }));

  sendLoading = () => {
    ctrl.setLoading(true);
    this.send.apply(this, arguments);
  };

  private reload(o, isRetry?: boolean) {
    // avoid reload if possible!
    if (o && o.t) {
      ctrl.setLoading(false);
      this.handlers[o.t](o.d);
    }
    else xhr.reload(this.ctrl).then(data => {
      if (li.socket.getVersion() > data.player.version) {
        // race condition! try to reload again
        if (isRetry) li.reload(); // give up and reload the page
        else this.reload(o, true);
      }
      else ctrl.reload(data);
    });
  };

  handlers: Handlers = {
    takebackOffers(o) {
      ctrl.setLoading(false);
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      ctrl.redraw();
    },
    move(o) {
      o.isMove = true;
      ctrl.apiMove(o);
    },
    drop(o) {
      o.isDrop = true;
      ctrl.apiMove(o);
    },
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
      ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color;
      ctrl.redraw();
    },
    rematchTaken(nextId) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by) {
      ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
      ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color;
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
        ctrl.userId &&
        ctrl.data.simul &&
        ctrl.userId == ctrl.data.simul.hostId &&
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
}
