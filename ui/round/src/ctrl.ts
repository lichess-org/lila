import * as round from './round';
import * as game from 'game';
import * as status from 'game/status';
import * as ground from './ground';
import notify from 'common/notification';
import { make as makeSocket, RoundSocket } from './socket';
import * as title from './title';
import * as blur from './blur';
import * as speech from './speech';
import * as cg from 'draughtsground/types';
import { Config as CgConfig } from 'draughtsground/config';
import { Api as DgApi } from 'draughtsground/api';
import { countGhosts } from 'draughtsground/fen';
import { ClockController } from './clock/clockCtrl';
import { CorresClockController, ctrl as makeCorresClock } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import sound = require('./sound');
import util = require('./util');
import xhr = require('./xhr');
import { ctrl as makeKeyboardMove, KeyboardMove } from './keyboardMove';
import renderUser = require('./view/user');
import cevalSub = require('./cevalSub');
import * as keyboard from './keyboard';
import { decSimulToMove } from './simulStanding';
import { RoundOpts, RoundData, ApiMove, ApiEnd, Redraw, SocketMove, SocketDrop, SocketOpts, MoveMetadata, Position, NvuiPlugin } from './interfaces';

interface GoneBerserk {
  white?: boolean;
  black?: boolean;
}

type Timeout = number;

const li = window.lidraughts;

export default class RoundController {

  data: RoundData;
  socket: RoundSocket;
  draughtsground: DgApi;
  clock?: ClockController;
  corresClock?: CorresClockController;
  trans: Trans;
  noarg: TransNoArg;
  keyboardMove?: KeyboardMove;
  moveOn: MoveOn;

  /**
   * We make a strict disctiontion between this.data.game.turns as the game state, determining turn color etc, and this.ply, determining the game view only
   * Rewrite what variable is used and/or updated where necessary, so that we can safely add "virtual plies" to this.ply
   */
  ply: number;
  firstSeconds: boolean = true;
  flip: boolean = false;
  loading: boolean = false;
  loadingTimeout: number;
  redirecting: boolean = false;
  moveToSubmit?: SocketMove;
  dropToSubmit?: SocketDrop;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  timeOutConfirm?: boolean = false;
  // will be replaced by view layer
  autoScroll: () => void = $.noop;
  challengeRematched: boolean = false;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
  shouldSendMoveTime: boolean = false;
  lastDrawOfferAtPly?: Ply;
  nvui?: NvuiPlugin;

  private music?: any;

  constructor(readonly opts: RoundOpts, readonly redraw: Redraw) {

    opts.data.steps = round.mergeSteps(opts.data.steps, this.isAlgebraic(opts.data) ? 1 : 0);
    round.massage(opts.data);

    const d = this.data = opts.data;

    this.ply = round.lastPly(d);

    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;

    setTimeout(() => { this.firstSeconds = false; this.redraw(); }, 3000);

    this.socket = makeSocket(opts.socketSend, this);

    if (li.RoundNVUI) this.nvui = li.RoundNVUI(redraw) as NvuiPlugin;

    if (d.clock) this.clock = new ClockController(d, {
      onFlag: () => { this.socket.outoftime(); this.redraw(); },
      soundColor: (d.simul || d.player.spectator || !d.pref.clockSound) ? undefined : d.player.color,
      nvui: !!this.nvui
    });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'lidraughts.move_on');

    this.trans = li.trans(opts.i18n);
    this.noarg = this.trans.noarg;

    if (this.data.simul && this.data.simul.timeOutUntil) {
      const curMillis = (new Date).getTime();
      if (this.data.simul.timeOutUntil > curMillis)
        setTimeout(this.redraw, this.data.simul.timeOutUntil - curMillis);
    }

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer || document.referrer.indexOf('/service-worker.js') === -1)
      setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    li.pubsub.on('jump', ply => { this.jump(parseInt(ply)); this.redraw(); });

    li.pubsub.on('sound_set', set => {
      if (!this.music && set === 'music')
        li.loadScript('javascripts/music/play.js').then(() => {
            this.music = li.playMusic();
        });
      if (this.music && set !== 'music') this.music = undefined;
    });

    li.pubsub.on('zen', () => {
      if (this.isPlaying()) {
        const zen = !$('body').hasClass('zen');
        $('body').toggleClass('zen', zen);
        li.dispatchEvent(window, 'resize');
        $.post('/pref/zen', { zen: zen ? 1 : 0 });
      }
    });

    if (li.ab && this.isPlaying()) li.ab.init(this);
  }

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  }

  private onUserMove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    if (li.ab && (!this.keyboardMove || !this.keyboardMove.usedSan)) li.ab.move(this, meta);
    this.sendMove(orig, dest, undefined, meta);
    // clear active timeout when host plays a move
    if (this.data.simul && this.data.simul.timeOutUntil && this.canTimeOut() && (new Date).getTime() < this.data.simul.timeOutUntil)
      this.moveOn.timeOutGame(0);
  };

  private onMove = (_orig: cg.Key, _dest: cg.Key, captured?: cg.Piece) => {
    if (captured) sound.capture();
    else sound.move();
  };

  private isSimulHost = () => {
    return this.data.simul && this.data.simul.hostId === this.opts.userId;
  };

  makeCgHooks = () => ({
    onUserMove: this.onUserMove,
    onMove: this.onMove,
    onNewPiece: sound.move,
  });

  replaying = (): boolean => this.ply !== round.lastPly(this.data);

  userJump = (ply: Ply): void => {
    this.cancelMove();
    this.draughtsground.selectSquare(null);
    if (ply != this.ply && this.jump(ply)) speech.userJump(this, this.ply);
    else this.redraw();
  };

  isPlaying = () => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(round.firstPly(this.data), Math.min(round.lastPly(this.data), ply));
    const plyDiff = Math.abs(ply - this.ply);
    this.ply = ply;
    this.justDropped = undefined;
    const s = this.stepAt(ply),
      ghosts = countGhosts(s.fen),
      config: CgConfig = {
        fen: s.fen,
        lastMove: util.uci2move(s.uci),
        turnColor: (this.ply - (ghosts == 0 ? 0 : 1)) % 2 === 0 ? 'white' : 'black'
      };
    if (this.replaying()) this.draughtsground.stop();
    else {
      config.movable = {
          color: this.isPlaying() ? this.data.player.color : undefined,
          dests: util.parsePossibleMoves(this.data.possibleMoves)
      }
      config.captureLength = this.data.captureLength;
    }
    this.draughtsground.set(config, plyDiff > 1);
    if (s.san && plyDiff !== 0) {
      if (s.san.includes('x')) sound.capture();
      else sound.move();
    }
    this.autoScroll();
    if (this.keyboardMove) this.keyboardMove.update(s);
    li.pubsub.emit('ply', ply);
    return true;
  };

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited' || d.game.speed === 'correspondence')
    );
  };

  private isAlgebraic = (d: RoundData): boolean => {
    return d.pref.coordSystem === 1 && d.game.variant.board.key === '64';
  }
  
  coordSystem = (): number => {
    return this.isAlgebraic(this.data) ? 1 : 0;
  }

  isLate = () => this.replaying() && status.playing(this.data);

  playerAt = (position: Position) =>
    (this.flip as any) ^ ((position === 'top') as any) ? this.data.opponent : this.data.player;

  flipNow = () => {
    this.flip = !this.nvui && !this.flip;
    this.draughtsground.set({
      orientation: ground.boardOrientation(this.data, this.flip)
    });
    this.redraw();
  };

  //Whos turn / game over in window title
  setTitle = () => title.set(this);

  actualSendMove = (type: string, action: any, meta: MoveMetadata = {}) => {
    const socketOpts: SocketOpts = {
      ackable: true
    };
    if (this.clock) {
      socketOpts.withLag = !this.shouldSendMoveTime || !this.clock.isRunning;
      if (meta.premove && this.shouldSendMoveTime) {
        this.clock.hardStopClock();
        socketOpts.millis = 0;
      } else {
        const moveMillis = this.clock.stopClock();
        if (moveMillis !== undefined && this.shouldSendMoveTime) {
          socketOpts.millis = moveMillis;
          if (socketOpts.millis < 3) {
            // instant move, no premove? might be fishy
            $.post('/jslog/' + this.data.game.id + this.data.player.id + '?n=instamove:' + Math.round(socketOpts.millis));
          }
        }
      }
    }
    this.socket.send(type, action, socketOpts);

    this.justDropped = meta.justDropped;
    this.justCaptured = meta.justCaptured;
    this.redraw();
  }

  sendMove = (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta: cg.MoveMetadata) => {
    const move: SocketMove = {
        u: orig + dest
    };
    //if (prom) move.u += (prom === 'knight' ? 'n' : prom[0]);
    if (prom) move.u += "";
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premove) {
      this.moveToSubmit = move;
      this.redraw();
    } else {
      this.actualSendMove('move', move, {
        justCaptured: meta.captured,
        premove: meta.premove
      })
    }
  };

  showYourMoveNotification = () => {
    const d = this.data;
    if (game.isPlayerTurn(d)) notify(() => {
      let txt = this.trans('yourTurn'),
          opponent = renderUser.userTxt(this, d.opponent);
      if (this.ply < 1) txt = opponent + '\njoined the game.\n' + txt;
      else {
        let move = d.steps[d.steps.length - 1].san,
            turn = Math.floor((this.ply - 1) / 2) + 1;
        move = turn + (this.ply % 2 === 1 ? '.' : '...') + ' ' + move;
        txt = opponent + '\nplayed ' + move + '.\n' + txt;
      }
      return txt;
    });
    else if (this.isPlaying() && this.ply < 1) notify(() => {
      return renderUser.userTxt(this, d.opponent) + '\njoined the game.';
    });
  };

  playerByColor = (c: Color) =>
    this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): void => {
    const d = this.data,
      playing = this.isPlaying(),
      ghosts = countGhosts(o.fen);

    d.game.turns = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'white' : 'black';

    const playedColor = o.ply % 2 === 0 ? 'black' : 'white',
      activeColor = d.player.color === d.game.player;

    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;

    this.playerByColor('white').offeringDraw = o.wDraw;
    this.playerByColor('black').offeringDraw = o.bDraw;

    d.possibleMoves = activeColor ? o.dests : undefined;
    d.captureLength = o.captLen;

    this.setTitle();

    if (!this.replaying()) {

      //Show next ply if we're following the head of the line (not replaying)
      this.ply = d.game.turns + (ghosts > 0 ? 1 : 0);

      if (o.role) this.draughtsground.newPiece({
        role: o.role,
        color: playedColor
      }, o.uci.substr(o.uci.length - 2, 2) as cg.Key);
      else {
        const keys = util.uci2move(o.uci);
        this.draughtsground.move(keys![0], keys![1], ghosts === 0);
      }
      this.draughtsground.set({
        turnColor: d.game.player,
        movable: {
          dests: playing ? util.parsePossibleMoves(d.possibleMoves) : {}
        },
        captureLength: d.captureLength
      });
      if (o.check) sound.check();
      blur.onMove();
      li.pubsub.emit('ply', this.ply);
    }
    d.game.threefold = !!o.threefold;

    const step = round.addStep(d.steps, {
      ply: d.game.turns,
      fen: o.fen,
      san: o.san,
      uci: o.uci
    }, this.coordSystem());

    this.justDropped = undefined;
    this.justCaptured = undefined;
    game.setOnGame(d, playedColor, true);
    this.data.forecastCount = undefined;
    if (o.clock) {
      const oc = o.clock;
      this.shouldSendMoveTime = true;
      const delay = (playing && activeColor) ? 0 : (oc.lag || 1);
      if (this.clock) this.clock.setClock(d, oc.white, oc.black, delay);
      else if (this.corresClock) this.corresClock.update(
        oc.white,
        oc.black);
    }
    if (this.data.expiration) {
      if (this.data.steps.length > 2) this.data.expiration = undefined;
      else this.data.expiration.movedAt = Date.now();
    }
    this.redraw();
    if (playing && playedColor == d.player.color) {
      this.moveOn.next();
      cevalSub.publish(d, o);
      if (this.data.simul && !this.redirecting)
        decSimulToMove(this.trans);
    }
    if (!this.replaying() && playedColor != d.player.color) {
      // TODO: atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      const premoveDelay = 1;
      setTimeout(() => {
        if (!this.draughtsground.playPremove()) {
          this.showYourMoveNotification();
        }
      }, premoveDelay);
    }
    this.autoScroll();
    this.onChange();
    if (this.keyboardMove) this.keyboardMove.update(step, playedColor != d.player.color);
    if (this.music) this.music.jump(o);
    speech.step(o);
  };

  private clearJust() {
    this.justDropped = undefined;
    this.justCaptured = undefined;
  }

  reload = (d: RoundData): void => {
    d.steps = round.mergeSteps(d.steps, this.coordSystem());
    if (d.steps.length !== this.data.steps.length) this.ply = d.steps[d.steps.length - 1].ply;
    round.massage(d);
    this.data = d;
    this.clearJust();
    this.shouldSendMoveTime = false;
    if (this.clock) this.clock.setClock(d, d.clock!.white, d.clock!.black);
    if (this.corresClock) this.corresClock.update(d.correspondence.white, d.correspondence.black);
    if (!this.replaying()) ground.reload(this);
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.redraw();
    this.autoScroll();
    this.onChange();
    this.setLoading(false);
    if (this.keyboardMove) this.keyboardMove.update(d.steps[d.steps.length - 1]);
  };

  endWithData = (o: ApiEnd): void => {
    const d = this.data;
    d.game.winner = o.winner;
    d.game.status = o.status;
    d.game.boosted = o.boosted;
    this.userJump(round.lastPly(d));
    this.draughtsground.stop();
    if (o.ratingDiff) {
      d.player.ratingDiff = o.ratingDiff[d.player.color];
      d.opponent.ratingDiff = o.ratingDiff[d.opponent.color];
    }
    if (!d.player.spectator && d.game.turns > 1)
      li.sound[o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw']();
    this.clearJust();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock) this.clock.setClock(d, o.clock.wc * .01, o.clock.bc * .01);
    this.redraw();
    this.autoScroll();
    this.onChange();
    if (d.tv) setTimeout(li.reload, 10000);
    speech.status(this);
  };

  challengeRematch = (): void => {
    this.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(() => {
      li.challengeApp.open();
      if (li.once('rematch-challenge')) setTimeout(() => {
        li.hopscotch(function () {
          window.hopscotch.configure({
            i18n: { doneBtn: 'OK, got it' }
          }).startTour({
            id: "rematch-challenge",
            showPrevButton: true,
            steps: [{
              title: "Challenged to a rematch",
              content: 'Your opponent is offline, but they can accept this challenge later!',
              target: "#challenge-app",
              placement: "bottom"
            }]
          });
        });
      }, 1000);
    }, _ => {
      this.challengeRematched = false;
    });
  };

  private makeCorrespondenceClock = (): void => {
    if (this.data.correspondence && !this.corresClock)
      this.corresClock = makeCorresClock(
        this,
        this.data.correspondence,
        this.socket.outoftime
      );
  };

  private corresClockTick = (): void => {
    if (this.corresClock && game.playable(this.data))
      this.corresClock.tick(this.data.game.player);
  };

  private setQuietMode = () => {
    const was = li.quietMode;
    const is = this.isPlaying();
    if (was !== is) {
      li.quietMode = is;
      $('body')
        .toggleClass('playing', is)
        .toggleClass('no-select',
        is && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5);
    }
  };

  takebackYes = () => {
    this.socket.sendLoading('takeback-yes');
    this.draughtsground.cancelPremove();
  };

  resign = (v: boolean): void => {
    if (v) {
      if (this.resignConfirm || !this.data.pref.confirmResign) {
        this.socket.sendLoading('resign');
        clearTimeout(this.resignConfirm);
      } else {
        this.resignConfirm = setTimeout(() => this.resign(false), 3000);
      }
      this.redraw();
    } else if (this.resignConfirm) {
      clearTimeout(this.resignConfirm);
      this.resignConfirm = undefined;
      this.redraw();
    }
  }

  goBerserk = () => {
    this.socket.berserk();
    li.sound.berserk();
  };

  setBerserk = (color: Color): void => {
    if (this.goneBerserk[color]) return;
    this.goneBerserk[color] = true;
    if (color !== this.data.player.color) li.sound.berserk();
    this.redraw();
  };

  setLoading = (v: boolean, duration: number = 1500) => {
    clearTimeout(this.loadingTimeout);
    if (v) {
      this.loading = true;
      this.loadingTimeout = setTimeout(() => {
        this.loading = false;
        this.redraw();
      }, duration);
      this.redraw();
    } else if (this.loading) {
      this.loading = false;
      this.redraw();
    }
  };

  setRedirecting = () => {
    this.redirecting = true;
    setTimeout(() => {
      this.redirecting = false;
      this.redraw();
    }, 2500);
    this.redraw();
  };

  submitMove = (v: boolean): void => {
    const toSubmit = this.moveToSubmit || this.dropToSubmit;
    if (v && toSubmit) {
      if (this.moveToSubmit) this.actualSendMove('move', this.moveToSubmit);
      else this.actualSendMove('drop', this.dropToSubmit);
      li.sound.confirmation();
    } else this.jump(this.ply);
    this.cancelMove();
    if (toSubmit) this.setLoading(true, 300);
  };

  cancelMove = (): void => {
    this.moveToSubmit = undefined;
    this.dropToSubmit = undefined;
  };

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  forceResignable = (): boolean => {
    const d = this.data;
    return !d.opponent.ai &&
      game.isForceResignable(d) &&
      !!d.clock &&
      d.opponent.isGone &&
      !game.isPlayerTurn(d) &&
      game.resignable(d);
  }

  canOfferDraw = (): boolean =>
    game.drawable(this.data) && (this.lastDrawOfferAtPly || -99) < (this.ply - 20);

  offerDraw = (v: boolean): void => {
    if (this.canOfferDraw()) {
      if (this.drawConfirm) {
        if (v) this.doOfferDraw();
        clearTimeout(this.drawConfirm);
        this.drawConfirm = undefined;
      } else if (v) {
        if (this.data.pref.confirmResign) this.drawConfirm = setTimeout(() => {
          this.offerDraw(false);
        }, 3000);
        else this.doOfferDraw();
      }
    }
    this.redraw();
  };

  private doOfferDraw = () => {
    this.lastDrawOfferAtPly = this.ply;
    this.socket.sendLoading('draw-yes', null)
  };

  canTimeOut = () =>
    this.isSimulHost() && this.isPlaying()

  setDraughtsground = (dg: DgApi) => {
    this.draughtsground = dg;
    if (this.data.pref.keyboardMove && !window.lidraughts.hasTouchEvents) {
      this.keyboardMove = makeKeyboardMove(this, this.stepAt(this.ply), this.redraw);
      li.raf(this.redraw);
    }
  };

  stepAt = (ply: Ply) => round.plyStep(this.data, ply);

  private delayedInit = () => {
    const d = this.data;
    if (this.isPlaying() && game.nbMoves(d, d.player.color) === 0 && !this.isSimulHost()) {
      li.sound.genericNotify();
    }
    li.requestIdleCallback(() => {
      if (this.isPlaying()) {
        if (!d.simul || d.simul.isUnique) blur.init(d.steps.length > 2);

        title.init();
        this.setTitle();

        window.addEventListener('beforeunload', e => {
          if (li.hasToReload ||
            this.nvui ||
            !game.playable(d) ||
            !d.clock ||
            d.opponent.ai ||
            this.isSimulHost()) return;
          this.socket.send('bye2');
          const msg = 'There is a game in progress!';
          (e || window.event).returnValue = msg;
          return msg;
        });

        if (!this.nvui && d.pref.submitMove) {
          window.Mousetrap.bind('esc', () => {
            this.submitMove(false);
            this.draughtsground.cancelMove();
          });
          window.Mousetrap.bind('return', () => this.submitMove(true));
        }
        cevalSub.subscribe(this);
      }

      if (!this.nvui) keyboard.init(this);

      speech.setup(this);

      this.onChange();
    });
  };

}
