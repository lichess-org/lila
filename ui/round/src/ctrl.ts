import * as round from './round';
import { game, status } from 'game';
import * as ground from './ground';
import RoundSocket from './socket';
import * as title from './title';
import * as promotion from './promotion';
import * as blur from './blur';
import * as blind from './blind';
import * as cg from 'chessground/types';
import { Config as CgConfig } from 'chessground/config';
import { Api as CgApi } from 'chessground/api';
import { ctrl as makeClock, ClockController } from './clock/clockCtrl';
import { ctrl as makeCorresClock } from './corresClock/corresClockCtrl';
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

import { RoundOpts, RoundData, ApiMove, ApiEnd, Redraw } from './interfaces';

interface GoneBerserk {
  white?: boolean;
  black?: boolean;
}

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

  ply: number;
  firstSeconds: boolean = true;
  flip: boolean = false;
  loading: boolean = false;
  loadingTimeout?: number;
  redirecting: false;
  moveToSubmit?: SocketMove;
  dropToSubmit?: SocketDrop;
  goneBerserk: GoneBerserk = {};
  resignConfirm: boolean = false;
  autoScroll: () => void = $.noop; // will be replaced by view layer
    challengeRematched: boolean = false;
  justDropped?: any;
  justCaptured?: any;
  justMoved: boolean = false;
  preDrop?: cg.Role;
  lastDrawOfferAtPly?: Ply;
  lastMoveMillis?: number;

  private timer = window.performance ? window.performance.now.bind(performance) : Date.now;
  private music?: any;

  constructor(opts: RoundOpts, redraw: Redraw) {

    const d = round.merge({}, opts.data).data;

    this.opts = opts;
    this.data = d;
    this.redraw = redraw;

    this.ply = round.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;

    setTimeout(() => { this.firstSeconds = false; this.redraw(); }, 3000);

    this.socket = new RoundSocket(opts.socketSend, this);

    if (d.clock) this.clock = makeClock(d.clock, {
      onFlag() { this.socket.outoftime(); this.redraw(); },
      soundColor: (d.simul || d.player.spectator || !d.pref.clockSound) ? null : d.player.color
    });
    else this.makeCorrespondenceClock();

    if (this.clock) {
      const tickNow = () => {
        clockTick();
        if (game.playable(this.data)) setTimeout(tickNow, 100);
      };
      setTimeout(tickNow, 100);
    } else setInterval(this.corresClockTick, 1000);

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'lichess.move_on');

    this.trans = li.trans(opts.i18n);

    setTimeout(this.delayedInit, 200);

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

  private onUserMove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    if (li.ab && (!this.keyboardMove || !this.keyboardMove.usedSan)) li.ab(this, meta);
    if (!this.promotion.start(this, orig, dest, meta)) this.sendMove(orig, dest, false, meta);
  };

  private onUserNewPiece = (role: cg.Role, key: cg.Key, meta: cg.MoveMetadata) => {
    if (!this.replaying() && crazyValid(this.data, role, key)) {
      this.sendNewPiece(role, key, meta.predrop);
    } else this.jump(this.ply);
  };

  private onMove = (orig: cg.Key, dest: cg.Key, captured: boolean) => {
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

  private onPredrop = (role: cg.Role) => {
    this.preDrop = role;
    this.redraw();
  };

  makeCgHooks = () => ({
    onUserMove: onUserMove,
    onUserNewPiece: onUserNewPiece,
    onMove: onMove,
    onNewPiece: sound.move,
    onPremove: onPremove,
    onCancelPremove: onCancelPremove,
    onPredrop: onPredrop
  });

  replaying = (): boolean => this.ply !== round.lastPly(this.data);

  userJump = (ply: Ply): void => {
    this.cancelMove();
    this.chessground.selectSquare(null);
    this.jump(ply);
  };

  jump = (ply: Ply): boolean => {
    if (ply < round.firstPly(this.data) || ply > round.lastPly(this.data)) return false;
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
      color: game.isPlayerPlaying(this.data) && this.data.player.color,
      dests: util.parsePossibleMoves(this.data.possibleMoves)
    }
    this.chessground.set(config);
    if (s.san) {
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

  flip = () => {
    this.flip = !this.flip;
    this.chessground.set({
      orientation: ground.boardOrientation(this.data, this.flip)
    });
    this.redraw();
  };

  setTitle = () => title.set(this);

  actualSendMove = (type: string, action: any, meta: cg.MoveMetadata = {}) => {
    const socketOpts = {
      ackable: true
    }
    if (meta.premove) socketOpts.millis = 0;
    else if (this.lastMoveMillis !== null) {
      socketOpts.millis = this.timer() - this.lastMoveMillis;
      if (socketOpts.millis < 3) {
        // instant move, no premove? might be fishy
        $.post('/jslog/' + this.data.game.id + this.data.player.id + '?n=instamove:' + socketOpts.millis);
        delete socketOpts.millis;
      }
    }
    this.socket.send(type, action, socketOpts);

    this.justDropped = meta.justDropped;
    this.justCaptured = meta.justCaptured;
    this.preDrop = undefined;
    this.clockTick(); // A hack. Better is clock.update with millis above removed.
    this.justMoved = true;
    this.redraw();
  }

  sendMove = (orig: cg.Key, dest: cg.Key, prom?: cg.Role, meta: cg.MoveMetadata) => {
    const move: SocketMove = {
      u: orig + dest
    };
    if (prom) move.u += (prom === 'knight' ? 'n' : prom[0]);
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premove) {
      this.moveToSubmit = move;
      redraw();
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
    else if (game.isPlayerPlaying(d) && this.ply < 1) li.desktopNotification(() => {
      return renderUser.userTxt(this, d.opponent) + '\njoined the game.';
    });
  };

  private playerByColor = (c: Color) =>
    this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): void => {
    const d = this.data,
    playing = game.isPlayerPlaying(d);

    if (playing) this.lastMoveMillis = timer();
    d.game.turns = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'white' : 'black';
    const playedColor = o.ply % 2 === 0 ? 'black' : 'white',
    activeColor = d.player.color === d.game.player;
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    playerByColor('white').offeringDraw = o.wDraw;
    playerByColor('black').offeringDraw = o.bDraw;
    d.possibleMoves = activeColor ? o.dests : null;
    d.possibleDrops = activeColor ? o.drops : null;
    d.crazyhouse = o.crazyhouse;
    this.setTitle();
    if (!this.replaying()) {
      this.ply++;
      if (o.isMove) {
        const keys = util.uci2move(o.uci);
        this.chessground.move(keys[0], keys[1]);
      }
      else this.chessground.newPiece({
        role: o.role,
        color: playedColor
      }, o.uci.substr(2, 2));
      if (o.enpassant) {
        const p = o.enpassant, pieces = {};
        pieces[p.key] = false;
        this.chessground.setPieces(pieces);
        if (d.game.variant.key === 'atomic') {
          atomic.enpassant(this, p.key, p.color);
          sound.explode();
        } else sound.capture();
      }
      if (o.promotion) ground.promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      if (o.castle && !this.chessground.state.autoCastle) {
        const c = o.castle, pieces = {};
        pieces[c.king[0]] = false;
        pieces[c.rook[0]] = false;
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
    if (o.clock) {
      (this.clock || this.corresClock).update(o.clock.white, o.clock.black,
        playing && activeColor ? 0 : o.clock.lag);
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
    this.justMoved = false;
    game.setOnGame(d, playedColor, true);
    this.data.forecastCount = undefined;
    this.redraw();
    if (d.blind) blind.reload(this);
    if (playing && playedColor === d.player.color) {
      this.moveOn.next();
      cevalSub.publish(this, o);
    }
    if (!this.replaying() && playedColor !== d.player.color) {
      // atrocious hack to prevent race condition
      // with explosions and premoves
      // https://github.com/ornicar/lila/issues/343
      const premoveDelay = d.game.variant.key === 'atomic' ? 100 : 1;
      setTimeout(() => {
        if (!this.chessground.playPremove() && !playPredrop()) {
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

  private clearVmJust() {
    this.justDropped = undefined;
    this.justCaptured = undefined;
    this.justMoved = false;
    this.preDrop = undefined;
  }

  reload = (cfg: RoundData): void => {
    if (cfg.steps.length !== this.data.steps.length) this.ply = cfg.steps[cfg.steps.length - 1].ply;
    const merged = round.merge(this.data, cfg),
    d = merged.data;
    this.data = d;
    this.clearVmJust();
    if (this.clock) this.clock.update(d.clock.white, d.clock.black);
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
    if (merged.changes.drawOffer) li.desktopNotification(this.trans('yourOpponentOffersADraw'));
    if (merged.changes.takebackOffer) li.desktopNotification(this.trans('yourOpponentProposesATakeback'));
    if (merged.changes.rematchOffer) li.desktopNotification(this.trans('yourOpponentWantsToPlayANewGameWithYou'));
    if (this.keyboardMove) this.keyboardMove.update(cfg.steps[cfg.steps.length - 1]);
  };

  endWithData = (o: ApiEnd): void => {
    const d = this.data;
    d.game.winner = o.winner;
    d.game.status = o.status;
    d.game.boosted = o.boosted;
    this.chessground.stop();
    if (o.ratingDiff) {
      d.player.ratingDiff = o.ratingDiff[d.player.color];
      d.opponent.ratingDiff = o.ratingDiff[d.opponent.color];
    }
    if (!d.player.spectator && d.game.turns > 1)
    li.sound[o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw']();
    this.clearVmJust();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    this.redraw();
    this.autoScroll();
  };

  challengeRematch = (): void => {
    this.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(() => {
      li.challengeApp.open();
      if (li.once('rematch-challenge')) setTimeout(() => {
        li.hopscotch(function() {
          hopscotch.configure({
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
    }, data => {
      this.challengeRematched = false;
      $.modal(data.error);
    });
  };

  isClockRunning = (): boolean => {
    return this.data.clock && game.playable(this.data) && !this.justMoved &&
    ((this.data.game.turns - this.data.game.startedAtTurn) > 1 || this.data.clock.running);
  };

  private clockTick = (): void => {
    if (this.isClockRunning()) this.clock.tick(this, this.data.game.player);
  };

  private makeCorrespondenceClock = (): boolean => {
    if (this.data.correspondence && !this.corresClock)
    this.corresClock = new corresClockCtrl(
      this.data.correspondence,
      this.socket.outoftime
    );
  };

  private corresClockTick = (): void => {
    if (this.corresClock && game.playable(this.data))
    this.corresClock.tick(this.data.game.player);
  };

  private setQuietMode = () => {
    li.quietMode = game.isPlayerPlaying(this.data);
    document.body.classList.toggle('no-select',
      li.quietMode && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5);
  };

  takebackYes = () => {
    this.socket.sendLoading('takeback-yes');
    this.chessground.cancelPremove();
    promotion.cancel(this);
  };

  resign = (v: boolean): void => {
    if (this.resignConfirm) {
      if (v) this.socket.sendLoading('resign');
      else this.resignConfirm = false;
    } else if (v !== false) {
      if (this.data.pref.confirmResign) this.resignConfirm = true;
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
    if (v && (this.moveToSubmit || this.dropToSubmit)) {
      if (this.moveToSubmit) this.actualSendMove('move', this.moveToSubmit);
      else this.actualSendMove('drop', this.dropToSubmit);
      li.sound.confirmation();
    } else this.jump(this.ply);
    this.cancelMove();
    this.setLoading(true, 300);
  };

  cancelMove = (): void => {
    this.moveToSubmit = null;
    this.dropToSubmit = null;
  };

  forecastInfo = (): boolean => {
    const d = this.data;
    return game.isPlayerPlaying(d) && d.correspondence && !d.opponent.ai &&
    !this.replaying() && d.game.turns > 1 && li.once('forecast-info-seen6');
  }

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  forceResignable = (): boolean => {
    const d = this.data;
    return !d.opponent.ai &&
    d.clock &&
    d.opponent.isGone &&
    !game.isPlayerTurn(d) &&
    game.resignable(d);
  }

  canOfferDraw = (): boolean =>
    game.drawable(this.data) && (this.lastDrawOfferAtPly || -99) < (this.ply - 20);

  offerDraw = (): void => {
    if (this.canOfferDraw()) {
      this.lastDrawOfferAtPly = this.ply;
      this.socket.sendLoading('draw-yes', null)
    }
  };

  setChessground = (cg: CgApi) => {
    this.chessground = cg;
    if (this.data.pref.keyboardMove) {
      this.keyboardMove = makeKeyboardMove(cg, round.plyStep(this.data, this.ply), this.redraw);
    }
  };

  private delayedInit = () => {
    if (game.isPlayerPlaying(this.data) && game.nbMoves(this.data, this.data.player.color) === 0) {
      li.sound.genericNotify();
    }
    li.requestIdleCallback(() => {
      if (game.isPlayerPlaying(this.data)) {
        if (!this.data.simul) blur.init(this.data.steps.length > 2);

        title.init(this);
        this.setTitle();

        window.addEventListener('beforeunload', e => {
          if (!li.hasToReload && !this.data.blind && game.playable(this.data) && this.data.clock && !this.data.opponent.ai) {
            document.body.classList.remove('fpmenu');
            this.socket.send('bye2');
            const msg = 'There is a game in progress!';
            (e || window.event).returnValue = msg;
            return msg;
          }
        });

        window.Mousetrap.bind(['esc'], () => this.chessground.cancelMove());

        cevalSub.subscribe(this);
      }

      keyboard.init(this);

      this.onChange();

    });
  };
};
