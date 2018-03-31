import * as round from './round';
import { game, status } from 'game';
import * as ground from './ground';
import { make as makeSocket, RoundSocket } from './socket';
import * as title from './title';
import * as promotion from './promotion';
import * as blur from './blur';
import * as blind from './blind';
import * as cg from 'chessground/types';
import { Config as CgConfig } from 'chessground/config';
import { Api as CgApi } from 'chessground/api';
import { ClockController } from './clock/clockCtrl';
import { CorresClockController, ctrl as makeCorresClock } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import atomic = require('./atomic');
import sound = require('./sound');
import util = require('./util');
import xhr = require('./xhr');
import { valid as crazyValid } from './crazy/crazyCtrl';
import { ctrl as makeKeyboardMove, KeyboardMove } from './keyboardMove';
import renderUser = require('./view/user');
import cevalSub = require('./cevalSub');
import * as keyboard from './keyboard';

import { RoundOpts, RoundData, ApiMove, ApiEnd, Redraw, SocketMove, SocketDrop, SocketOpts, MoveMetadata } from './interfaces';

interface GoneBerserk {
  white?: boolean;
  black?: boolean;
};

type Timeout = number;

const li = window.lichess;

export default class RoundController {

  opts: RoundOpts;
  data: RoundData;
  redraw: Redraw;
  socket: RoundSocket;
  chessground: CgApi;
  clock?: ClockController;
  corresClock?: CorresClockController;
  trans: Trans;
  keyboardMove?: KeyboardMove;
  moveOn: MoveOn;

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
  // will be replaced by view layer
  autoScroll: () => void = $.noop;
  challengeRematched: boolean = false;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
  shouldSendMoveTime: boolean = false;
  preDrop?: cg.Role;
  lastDrawOfferAtPly?: Ply;

  private music?: any;

  constructor(opts: RoundOpts, redraw: Redraw) {

    round.massage(opts.data);

    const d = this.data = opts.data;

    this.opts = opts;
    this.redraw = redraw;

    this.ply = round.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;

    setTimeout(() => { this.firstSeconds = false; this.redraw(); }, 3000);

    this.socket = makeSocket(opts.socketSend, this);

    if (d.clock) this.clock = new ClockController(d, {
      onFlag: () => { this.socket.outoftime(); this.redraw(); },
      soundColor: (d.simul || d.player.spectator || !d.pref.clockSound) ? undefined : d.player.color
    });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'lichess.move_on');

    this.trans = li.trans(opts.i18n);

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    li.pubsub.on('jump', ply => { this.jump(parseInt(ply)); this.redraw(); });

    li.pubsub.on('sound_set', set => {
      if (!this.music && set === 'music')
        li.loadScript('/assets/javascripts/music/play.js').then(() => {
          this.music = window.lichessPlayMusic();
        });
        if (this.music && set !== 'music') this.music = undefined;
    });

  }

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  }

  private onUserMove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    if (li.ab && (!this.keyboardMove || !this.keyboardMove.usedSan)) li.ab(this, meta);
    if (!promotion.start(this, orig, dest, meta)) this.sendMove(orig, dest, undefined, meta);
  };

  private onUserNewPiece = (role: cg.Role, key: cg.Key, meta: cg.MoveMetadata) => {
    if (!this.replaying() && crazyValid(this.data, role, key)) {
      this.sendNewPiece(role, key, !!meta.predrop);
    } else this.jump(this.ply);
  };

  private onMove = (_: cg.Key, dest: cg.Key, captured?: cg.Piece) => {
    if (captured) {
      if (this.data.game.variant.key === 'atomic') {
        sound.explode();
        atomic.capture(this, dest);
      } else sound.capture();
    } else sound.move();
  };

  private onPremove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    promotion.start(this, orig, dest, meta);
  };

  private onCancelPremove = () => {
    promotion.cancelPrePromotion(this);
  };

  private onPredrop = (role: cg.Role | undefined, _?: Key) => {
    this.preDrop = role;
    this.redraw();
  };

  private isSimulHost = () => {
    return this.data.simul && this.data.simul.hostId === this.opts.userId;
  };

  makeCgHooks = () => ({
    onUserMove: this.onUserMove,
    onUserNewPiece: this.onUserNewPiece,
    onMove: this.onMove,
    onNewPiece: sound.move,
    onPremove: this.onPremove,
    onCancelPremove: this.onCancelPremove,
    onPredrop: this.onPredrop
  });

  replaying = (): boolean => this.ply !== round.lastPly(this.data);

  userJump = (ply: Ply): void => {
    this.cancelMove();
    this.chessground.selectSquare(null);
    this.jump(ply);
  };

  isPlaying = () => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    if (ply < round.firstPly(this.data) || ply > round.lastPly(this.data)) return false;
    const samePly = this.ply === ply;
    this.ply = ply;
    this.justDropped = undefined;
    this.preDrop = undefined;
    const s = round.plyStep(this.data, ply),
    config: CgConfig = {
      fen: s.fen,
      lastMove: util.uci2move(s.uci),
      check: !!s.check,
      turnColor: this.ply % 2 === 0 ? 'white' : 'black'
    };
    if (this.replaying()) this.chessground.stop();
    else config.movable = {
      color: this.isPlaying() ? this.data.player.color : undefined,
      dests: util.parsePossibleMoves(this.data.possibleMoves)
    }
    this.chessground.set(config);
    if (s.san && !samePly) {
      if (s.san.indexOf('x') !== -1) sound.capture();
      else sound.move();
      if (/[+#]/.test(s.san)) sound.check();
    }
    this.autoScroll();
    if (this.keyboardMove) this.keyboardMove.update(s);
    return true;
  };

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited' || d.game.speed === 'correspondence')
    );
  };

  isLate = () => this.replaying() && status.playing(this.data);

  flipNow = () => {
    this.flip = !this.flip;
    this.chessground.set({
      orientation: ground.boardOrientation(this.data, this.flip)
    });
    this.redraw();
  };

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
            $.post('/jslog/' + this.data.game.id + this.data.player.id + '?n=instamove:' + socketOpts.millis);
          }
        }
      }
    }
    this.socket.send(type, action, socketOpts);

    this.justDropped = meta.justDropped;
    this.justCaptured = meta.justCaptured;
    this.preDrop = undefined;
    this.redraw();
  }

  sendMove = (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta: cg.MoveMetadata) => {
    const move: SocketMove = {
      u: orig + dest
    };
    if (prom) move.u += (prom === 'knight' ? 'n' : prom[0]);
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

  sendNewPiece = (role: cg.Role, key: cg.Key, isPredrop: boolean): void => {
    const drop: SocketDrop = {
      role: role,
      pos: key
    };
    if (blur.get()) drop.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !isPredrop) {
      this.dropToSubmit = drop;
      this.redraw();
    } else {
      this.actualSendMove('drop', drop, {
        justDropped: role,
        premove: isPredrop
      });
    }
  };

  showYourMoveNotification = () => {
    const d = this.data;
    if (game.isPlayerTurn(d)) li.desktopNotification(() => {
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
    else if (this.isPlaying() && this.ply < 1) li.desktopNotification(() => {
      return renderUser.userTxt(this, d.opponent) + '\njoined the game.';
    });
  };

  private playerByColor = (c: Color) =>
    this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): void => {
    const d = this.data,
    playing = this.isPlaying();

    d.game.turns = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'white' : 'black';
    const playedColor = o.ply % 2 === 0 ? 'black' : 'white',
    activeColor = d.player.color === d.game.player;
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    this.playerByColor('white').offeringDraw = o.wDraw;
    this.playerByColor('black').offeringDraw = o.bDraw;
    d.possibleMoves = activeColor ? o.dests : undefined;
    d.possibleDrops = activeColor ? o.drops : undefined;
    d.crazyhouse = o.crazyhouse;
    this.setTitle();
    if (!this.replaying()) {
      this.ply++;
      if (o.role) this.chessground.newPiece({
        role: o.role,
        color: playedColor
      }, o.uci.substr(2, 2) as cg.Key);
      else {
        const keys = util.uci2move(o.uci);
        this.chessground.move(keys![0], keys![1]);
      }
      if (o.enpassant) {
        const p = o.enpassant, pieces: cg.PiecesDiff = {};
        pieces[p.key] = null;
        this.chessground.setPieces(pieces);
        if (d.game.variant.key === 'atomic') {
          atomic.enpassant(this, p.key, p.color);
          sound.explode();
        } else sound.capture();
      }
      if (o.promotion) ground.promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      if (o.castle && !this.chessground.state.autoCastle) {
        const c = o.castle, pieces: cg.PiecesDiff = {};
        pieces[c.king[0]] = null;
        pieces[c.rook[0]] = null;
        pieces[c.king[1]] = {
          role: 'king',
          color: c.color
        };
        pieces[c.rook[1]] = {
          role: 'rook',
          color: c.color
        };
        this.chessground.setPieces(pieces);
      }
      this.chessground.set({
        turnColor: d.game.player,
        movable: {
          dests: playing ? util.parsePossibleMoves(d.possibleMoves) : {}
        },
        check: !!o.check
      });
      if (o.check) sound.check();
      blur.onMove();
    }
    d.game.threefold = !!o.threefold;
    const step = {
      ply: round.lastPly(this.data) + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check,
      crazy: o.crazyhouse
    };
    d.steps.push(step);
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
    if (d.blind) blind.reload(this);
    if (playing && playedColor === d.player.color) {
      this.moveOn.next();
      cevalSub.publish(d, o);
    }
    if (!this.replaying() && playedColor !== d.player.color) {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      const premoveDelay = d.game.variant.key === 'atomic' ? 100 : 1;
      setTimeout(() => {
        if (!this.chessground.playPremove() && !this.playPredrop()) {
          promotion.cancel(this);
          this.showYourMoveNotification();
        }
      }, premoveDelay);
    }
    this.autoScroll();
    this.onChange();
    if (this.keyboardMove) this.keyboardMove.update(step);
    if (this.music) this.music.jump(o);
  };

  private playPredrop = () => {
    return this.chessground.playPredrop(drop => {
      return crazyValid(this.data, drop.role, drop.key);
    });
  };

  private clearJust() {
    this.justDropped = undefined;
    this.justCaptured = undefined;
    this.preDrop = undefined;
  }

  reload = (d: RoundData): void => {
    if (d.steps.length !== this.data.steps.length) this.ply = d.steps[d.steps.length - 1].ply;
    round.massage(d);
    this.data = d;
    this.clearJust();
    this.shouldSendMoveTime = false;
    if (this.clock) this.clock.setClock(d, d.clock!.white, d.clock!.black);
    if (this.corresClock) this.corresClock.update(d.correspondence.white, d.correspondence.black);
    if (!this.replaying()) ground.reload(this);
    this.setTitle();
    if (d.blind) blind.reload(this);
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
    this.chessground.stop();
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
    if (d.tv) setTimeout(li.reload, 8000);
  };

  challengeRematch = (): void => {
    this.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(() => {
      li.challengeApp.open();
      if (li.once('rematch-challenge')) setTimeout(() => {
        li.hopscotch(function() {
          window.hopscotch.configure({
            i18n: { doneBtn: 'OK, got it' }
          }).startTour({
            id: "rematch-challenge",
            showPrevButton: true,
            steps: [{
              title: "Challenged to a rematch",
              content: 'Your opponent is offline, but they can accept this challenge later!',
              target: "#challenge_app",
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
    this.chessground.cancelPremove();
    promotion.cancel(this);
  };

  resign = (v: boolean): void => {
    if (this.resignConfirm) {
      if (v) this.socket.sendLoading('resign');
      else {
        clearTimeout(this.resignConfirm);
        this.resignConfirm = undefined;
      }
    } else if (v) {
      if (this.data.pref.confirmResign) this.resignConfirm = setTimeout(() => {
        this.resign(false);
      }, 3000);
      else this.socket.sendLoading('resign');
    }
    this.redraw();
  };

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

  forecastInfo = (): boolean => {
    const d = this.data;
    return this.isPlaying() && d.correspondence && !d.opponent.ai &&
    !this.replaying() && d.game.turns > 1 && li.once('forecast-info-seen6');
  }

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  forceResignable = (): boolean => {
    const d = this.data;
    return !d.opponent.ai &&
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

  setChessground = (cg: CgApi) => {
    this.chessground = cg;
    if (this.data.pref.keyboardMove) {
      this.keyboardMove = makeKeyboardMove(this, round.plyStep(this.data, this.ply), this.redraw);
    }
  };

  toggleZen = () => {
    if (this.isPlaying()) {
      const zen = !$('body').hasClass('zen');
      $('body').toggleClass('zen', zen)
      $.post('/pref/zen', { zen: zen ? 1 : 0 });
    }
  }

  private delayedInit = () => {
    if (this.isPlaying() && game.nbMoves(this.data, this.data.player.color) === 0 && !this.isSimulHost()) {
      li.sound.genericNotify();
    }
    li.requestIdleCallback(() => {
      if (this.isPlaying()) {
        if (!this.data.simul) blur.init(this.data.steps.length > 2);

        title.init();
        this.setTitle();

        window.addEventListener('beforeunload', e => {
          if (li.hasToReload ||
            this.data.blind ||
            !game.playable(this.data) ||
            !this.data.clock ||
            this.data.opponent.ai ||
            this.isSimulHost()) return;
            document.body.classList.remove('fpmenu');
            this.socket.send('bye2');
            const msg = 'There is a game in progress!';
            (e || window.event).returnValue = msg;
            return msg;
        });

        window.Mousetrap.bind('esc', () => {
          this.submitMove(false);
          this.chessground.cancelMove();
        });

        window.Mousetrap.bind('return', () => this.submitMove(true));

        cevalSub.subscribe(this);
      }

      keyboard.init(this);

      this.onChange();

    });
  };
}
