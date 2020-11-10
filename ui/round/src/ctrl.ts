import * as ab from 'ab';
import * as round from './round';
import * as game from 'game';
import * as status from 'game/status';
import * as ground from './ground';
import notify from 'common/notification';
import { make as makeSocket, RoundSocket } from './socket';
import * as title from './title';
import * as promotion from './promotion';
import * as blur from './blur';
import * as speech from './speech';
import * as cg from 'chessground/types';
import { Config as CgConfig } from 'chessground/config';
import { Api as CgApi } from 'chessground/api';
import { ClockController } from './clock/clockCtrl';
import { CorresClockController, ctrl as makeCorresClock } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import TransientMove from './transientMove';
import * as atomic from './atomic';
import * as sound from './sound';
import * as util from './util';
import * as xhr from './xhr';
import { valid as crazyValid, init as crazyInit, onEnd as crazyEndHook } from './crazy/crazyCtrl';
import { ctrl as makeKeyboardMove, KeyboardMove } from './keyboardMove';
import * as renderUser from './view/user';
import * as cevalSub from './cevalSub';
import * as keyboard from './keyboard';

import { RoundOpts, RoundData, ApiMove, ApiEnd, Redraw, SocketMove, SocketDrop, SocketOpts, MoveMetadata, Position, NvuiPlugin } from './interfaces';

interface GoneBerserk {
  white?: boolean;
  black?: boolean;
}

type Timeout = number;

export default class RoundController {

  data: RoundData;
  socket: RoundSocket;
  chessground: CgApi;
  clock?: ClockController;
  corresClock?: CorresClockController;
  trans: Trans;
  noarg: TransNoArg;
  keyboardMove?: KeyboardMove;
  moveOn: MoveOn;

  ply: number;
  firstSeconds: boolean = true;
  flip: boolean = false;
  loading: boolean = false;
  loadingTimeout: number;
  redirecting: boolean = false;
  transientMove: TransientMove;
  moveToSubmit?: SocketMove;
  dropToSubmit?: SocketDrop;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  // will be replaced by view layer
  autoScroll: () => void = () => {};
  challengeRematched: boolean = false;
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
  shouldSendMoveTime: boolean = false;
  preDrop?: cg.Role;
  lastDrawOfferAtPly?: Ply;
  nvui?: NvuiPlugin;
  sign: string = Math.random().toString(36);
  private music?: any;

  constructor(readonly opts: RoundOpts, readonly redraw: Redraw) {

    round.massage(opts.data);

    const d = this.data = opts.data;

    this.ply = round.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;

    setTimeout(() => { this.firstSeconds = false; this.redraw(); }, 3000);

    this.socket = makeSocket(opts.socketSend, this);

    if (lichess.RoundNVUI) this.nvui = lichess.RoundNVUI(redraw) as NvuiPlugin;

    if (d.clock) this.clock = new ClockController(d, {
      onFlag: this.socket.outoftime,
      soundColor: (d.simul || d.player.spectator || !d.pref.clockSound) ? undefined : d.player.color,
      nvui: !!this.nvui
    });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'move-on');
    this.transientMove = new TransientMove(this.socket);

    this.trans = lichess.trans(opts.i18n);
    this.noarg = this.trans.noarg;

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer?.includes('/serviceWorker.'))
      setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    lichess.pubsub.on('jump', ply => { this.jump(parseInt(ply)); this.redraw(); });

    lichess.pubsub.on('sound_set', set => {
      if (!this.music && set === 'music')
        lichess.loadScript('javascripts/music/play.js').then(() => {
          this.music = lichess.playMusic();
        });
      if (this.music && set !== 'music') this.music = undefined;
    });

    lichess.pubsub.on('zen', () => {
      if (this.isPlaying()) {
        const zen = !$('body').hasClass('zen');
        $('body').toggleClass('zen', zen);
        window.dispatchEvent(new Event('resize'));
        xhr.setZen(zen);
      }
    });

    if (this.isPlaying()) ab.init(this);
  }

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  }

  private onUserMove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    if (!this.keyboardMove || !this.keyboardMove.usedSan) ab.move(this, meta);
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
    if (ply != this.ply && this.jump(ply)) speech.userJump(this, this.ply);
    else this.redraw();
  };

  isPlaying = () => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(round.firstPly(this.data), Math.min(round.lastPly(this.data), ply));
    const isForwardStep = ply === this.ply + 1;
    this.ply = ply;
    this.justDropped = undefined;
    this.preDrop = undefined;
    const s = this.stepAt(ply),
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
    if (s.san && isForwardStep) {
      if (s.san.includes('x')) sound.capture();
      else sound.move();
      if (/[+#]/.test(s.san)) sound.check();
    }
    this.autoScroll();
    if (this.keyboardMove) this.keyboardMove.update(s);
    lichess.pubsub.emit('ply', ply);
    return true;
  };

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return d.pref.replay === 2 || (
      d.pref.replay === 1 && (d.game.speed === 'classical' || d.game.speed === 'unlimited' || d.game.speed === 'correspondence')
    );
  };

  isLate = () => this.replaying() && status.playing(this.data);

  playerAt = (position: Position) =>
    (this.flip as any) ^ ((position === 'top') as any) ? this.data.opponent : this.data.player;

  flipNow = () => {
    this.flip = !this.nvui && !this.flip;
    this.chessground.set({
      orientation: ground.boardOrientation(this.data, this.flip)
    });
    this.redraw();
  };

  setTitle = () => title.set(this);

  actualSendMove = (tpe: string, data: any, meta: MoveMetadata = {}) => {
    const socketOpts: SocketOpts = {
      sign: this.sign,
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
        }
      }
    }
    this.socket.send(tpe, data, socketOpts);

    this.justDropped = meta.justDropped;
    this.justCaptured = meta.justCaptured;
    this.preDrop = undefined;
    this.transientMove.register();
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
    if (game.isPlayerTurn(d)) notify(() => {
      let txt = this.noarg('yourTurn'),
        opponent = renderUser.userTxt(this, d.opponent);
      if (this.ply < 1) txt = `${opponent}\njoined the game.\n${txt}`;
      else {
        let move = d.steps[d.steps.length - 1].san,
          turn = Math.floor((this.ply - 1) / 2) + 1;
        move = `${turn}${this.ply % 2 === 1 ? '.' : '...'} ${move}`;
        txt = `${opponent}\nplayed ${move}.\n${txt}`;
      }
      return txt;
    });
    else if (this.isPlaying() && this.ply < 1) notify(() => 
      renderUser.userTxt(this, d.opponent) + '\njoined the game.'
    );
  };

  playerByColor = (c: Color) =>
    this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): true => {
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
        // This block needs to be idempotent, even for castling moves in
        // Chess960.
        const keys = util.uci2move(o.uci)!,
        pieces = this.chessground.state.pieces;
        if (!o.castle || (pieces.get(o.castle.king[0])?.role === 'king' && pieces.get(o.castle.rook[0])?.role === 'rook')) {
          this.chessground.move(keys[0], keys[1]);
        }
      }
      if (o.enpassant) {
        const p = o.enpassant;
        this.chessground.setPieces(new Map([[p.key, undefined]]));
        if (d.game.variant.key === 'atomic') {
          atomic.enpassant(this, p.key, p.color);
          sound.explode();
        } else sound.capture();
      }
      if (o.promotion) ground.promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      this.chessground.set({
        turnColor: d.game.player,
        movable: {
          dests: playing ? util.parsePossibleMoves(d.possibleMoves) : new Map(),
        },
        check: !!o.check
      });
      if (o.check) sound.check();
      blur.onMove();
      lichess.pubsub.emit('ply', this.ply);
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
      this.shouldSendMoveTime = true;
      const oc = o.clock,
        delay = (playing && activeColor) ? 0 : (oc.lag || 1);
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
      this.transientMove.clear();
      this.moveOn.next();
      cevalSub.publish(d, o);
    }
    if (!this.replaying() && playedColor != d.player.color) {
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
    if (this.keyboardMove) this.keyboardMove.update(step, playedColor != d.player.color);
    if (this.music) this.music.jump(o);
    speech.step(step);
    return true; // prevents default socket pubsub
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
    if (!d.player.spectator && d.game.turns > 1) {
      const key = o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw';
      lichess.sound.play(key);
      if (key != 'victory' && d.game.turns > 6 && !d.tournament && !d.swiss && lichess.storage.get('courtesy') == '1')
        this.opts.chat?.instance?.then(c => c.post('Good game, well played'));
    }
    if (d.crazyhouse) crazyEndHook();
    this.clearJust();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock) this.clock.setClock(d, o.clock.wc * .01, o.clock.bc * .01);
    this.redraw();
    this.autoScroll();
    this.onChange();
    if (d.tv) setTimeout(lichess.reload, 10000);
    speech.status(this);
  };

  challengeRematch = (): void => {
    this.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(() => {
      lichess.pubsub.emit('challenge-app.open');
      if (lichess.once('rematch-challenge')) setTimeout(() => {
        lichess.hopscotch(function() {
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
    const was = lichess.quietMode;
    const is = this.isPlaying();
    if (was !== is) {
      lichess.quietMode = is;
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

  resign = (v: boolean, immediately?: boolean): void => {
    if (v) {
      if (this.resignConfirm || !this.data.pref.confirmResign || immediately) {
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
    lichess.sound.play('berserk');
  };

  setBerserk = (color: Color): void => {
    if (this.goneBerserk[color]) return;
    this.goneBerserk[color] = true;
    if (color !== this.data.player.color) lichess.sound.play('berserk');
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
    lichess.unload.expected = true;
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
      lichess.sound.play('confirmation');
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

  private goneTick?: number;
  setGone = (gone: number | boolean) => {
    game.setGone(this.data, this.data.opponent.color, gone);
    clearTimeout(this.goneTick);
    if (Number(gone) > 1)
      this.goneTick = setTimeout(() => {
        const g = Number(this.opponentGone());
        if (g > 1) this.setGone(g - 1);
      }, 1000);
    this.redraw();
  }

  opponentGone = (): number | boolean => {
    const d = this.data;
    return d.opponent.gone !== false &&
      !game.isPlayerTurn(d) &&
      game.resignable(d) &&
      d.opponent.gone;
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
      this.keyboardMove = makeKeyboardMove(this, this.stepAt(this.ply), this.redraw);
      requestAnimationFrame(() => this.redraw());
    }
  };

  stepAt = (ply: Ply) => round.plyStep(this.data, ply);

  private delayedInit = () => {
    const d = this.data;
    if (this.isPlaying() && game.nbMoves(d, d.player.color) === 0 && !this.isSimulHost()) {
      lichess.sound.play('genericNotify');
    }
    lichess.requestIdleCallback(() => {
      const d = this.data;
      if (this.isPlaying()) {
        if (!d.simul) blur.init(d.steps.length > 2);

        title.init();
        this.setTitle();

        if (d.crazyhouse) crazyInit(this);

        window.addEventListener('beforeunload', e => {
          const d = this.data;
          if (lichess.unload.expected ||
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
            this.chessground.cancelMove();
          })
          .bind('return', () => this.submitMove(true));
        }
        cevalSub.subscribe(this);
      }

      if (!this.nvui) keyboard.init(this);

      speech.setup(this);

      this.onChange();
    }, 800);
  };
}
