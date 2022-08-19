import * as round from './round';
import * as game from 'game';
import * as status from 'game/status';
import * as ground from './ground';
import notify from 'common/notification';
import { make as makeSocket, RoundSocket } from './socket';
import * as title from './title';
import * as blur from './blur';
import * as speech from './speech';
import * as sg from 'shogiground/types';
import { Config as SgConfig } from 'shogiground/config';
import { Api as SgApi } from 'shogiground/api';
import { samePiece } from 'shogiground/util';
import { ClockController } from './clock/clockCtrl';
import { initialSfen } from 'shogiops/sfen';
import { CorresClockController, ctrl as makeCorresClock } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import TransientMove from './transientMove';
import * as sound from './sound';
import * as util from './util';
import * as xhr from './xhr';
import { ctrl as makeKeyboardMove, KeyboardMove } from './keyboardMove';
import * as renderUser from './view/user';
import * as cevalSub from './cevalSub';
import * as keyboard from './keyboard';

import {
  RoundOpts,
  RoundData,
  ApiMove,
  ApiEnd,
  Redraw,
  SocketOpts,
  MoveMetadata,
  Position,
  NvuiPlugin,
  SocketUsi,
} from './interfaces';
import { makeNotation, makeNotationLine } from 'common/notation';
import { isDrop, Role, Piece } from 'shogiops/types';
import { parseUsi, roleToString, makeSquare, opposite } from 'shogiops/util';
import { Shogiground } from 'shogiground';
import { usiToSquareNames } from 'shogiops/compat';
import { unpromote } from 'shogiops/variantUtil';

interface GoneBerserk {
  sente?: boolean;
  gote?: boolean;
}

type Timeout = number;

const li = window.lishogi;

export default class RoundController {
  data: RoundData;
  socket: RoundSocket;
  shogiground: SgApi;
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
  impasseHelp: boolean = false;
  transientMove: TransientMove;
  usiToSubmit?: SocketUsi;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  // will be replaced by view layer
  autoScroll: () => void = $.noop;
  challengeRematched: boolean = false;
  shouldSendMoveTime: boolean = false;
  lastDrawOfferAtPly?: Ply;
  nvui?: NvuiPlugin;

  private music?: any;

  constructor(readonly opts: RoundOpts, readonly redraw: Redraw) {
    round.massage(opts.data);

    const d = (this.data = opts.data);

    this.ply = round.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;

    this.shogiground = Shogiground(ground.makeConfig(this));

    this.initNotation();

    setTimeout(() => {
      this.firstSeconds = false;
      this.redraw();
    }, 3000);

    this.socket = makeSocket(opts.socketSend, this);

    if (li.RoundNVUI) this.nvui = li.RoundNVUI(redraw) as NvuiPlugin;

    if (d.clock)
      this.clock = new ClockController(d, {
        onFlag: this.socket.outoftime,
        redraw: this.redraw,
        soundColor: d.simul || d.player.spectator || !d.pref.clockSound ? undefined : d.player.color,
        nvui: !!this.nvui,
      });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'move-on');
    this.transientMove = new TransientMove(this.socket);

    this.trans = li.trans(opts.i18n);
    this.noarg = this.trans.noarg;

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer || document.referrer.indexOf('/service-worker.js') === -1)
      setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    li.pubsub.on('jump', ply => {
      this.jump(parseInt(ply));
      this.redraw();
    });

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
        xhr.setZen(zen);
      }
    });

    if (li.ab && this.isPlaying()) li.ab.init(this);
  }

  private initNotation = (): void => {
    if (this.data.steps.length <= 1) return;
    const usis = this.data.steps.slice(1).map(s => s.usi!),
      movesNotation: MoveNotation[] = makeNotationLine(
        this.data.pref.notation,
        this.data.game.initialSfen || initialSfen(this.data.game.variant.key),
        this.data.game.variant.key,
        usis
      );
    for (let i = 1; i < this.data.steps.length; i++) this.data.steps[i].notation = movesNotation[i - 1];
  };

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  };

  private onUserMove = (orig: sg.Key, dest: sg.Key, prom: boolean, meta: sg.MoveMetadata) => {
    if (li.ab && (!this.keyboardMove || !this.keyboardMove.usedMove)) li.ab.move(this, meta);
    this.sendMove(orig, dest, prom, meta);

    // to update hand immediately and not wait on apiMove
    if (meta.captured)
      this.shogiground.addToHand({
        role: unpromote(this.data.game.variant.key)(meta.captured.role) || meta.captured.role,
        color: opposite(meta.captured.color),
      });
  };

  private onUserDrop = (piece: Piece, key: sg.Key, _prom: boolean, meta: sg.MoveMetadata) => {
    if (li.ab && (!this.keyboardMove || !this.keyboardMove.usedMove)) li.ab.drop(this, meta);
    this.sendDrop(piece.role, key, meta);
  };

  private onMove = (_orig: sg.Key, _dest: sg.Key, _prom: boolean, captured?: Piece) => {
    if (captured) {
      sound.capture();
    } else sound.move();
  };

  private onDrop = (_piece: Piece, _key: sg.Key, _prom: boolean) => {
    sound.move();
  };

  private isSimulHost = () => {
    return this.data.simul && this.data.simul.hostId === this.opts.userId;
  };

  makeSgHooks = () => ({
    onUserMove: this.onUserMove,
    onUserDrop: this.onUserDrop,
    onMove: this.onMove,
    onDrop: this.onDrop,
  });

  replaying = (): boolean => this.ply !== round.lastPly(this.data);

  userJump = (ply: Ply): void => {
    this.cancelMove();
    this.shogiground.selectSquare(null);
    if (ply != this.ply && this.jump(ply)) speech.userJump(this, this.ply);
    else this.redraw();
  };

  isPlaying = () => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(round.firstPly(this.data), Math.min(round.lastPly(this.data), ply));
    const isForwardStep = ply === this.ply + 1;
    this.ply = ply;
    const s = this.stepAt(ply),
      splitSfen = s.sfen.split(' '),
      config: SgConfig = {
        sfen: { board: splitSfen[0], hands: splitSfen[2] },
        lastDests: s.usi ? (usiToSquareNames(s.usi) as Key[]) : undefined,
        check: !!s.check,
        turnColor: this.ply % 2 === 0 ? 'sente' : 'gote',
        activeColor: this.isPlaying() ? this.data.player.color : undefined,
      };
    if (this.replaying()) this.shogiground.stop();
    else {
      config.movable = {
        dests: this.isPlaying() ? util.getMoveDests(s.sfen, this.data.game.variant.key) : new Map(),
      };
      config.droppable = {
        dests: this.isPlaying() ? util.getDropDests(s.sfen, this.data.game.variant.key) : new Map(),
      };
    }
    const move = s.usi && parseUsi(s.usi),
      capture = isForwardStep && move && this.shogiground.state.pieces.get(makeSquare(move.to) as Key);
    this.shogiground.set(config);
    if (s.usi && isForwardStep) {
      if (capture) sound.capture();
      else sound.move();
      if (s.check) sound.check();
    }
    this.autoScroll();
    if (this.keyboardMove) this.keyboardMove.update(s);
    li.pubsub.emit('ply', ply);
    return true;
  };

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return (
      d.pref.replay === 2 ||
      (d.pref.replay === 1 &&
        (d.game.speed === 'classical' || d.game.speed === 'unlimited' || d.game.speed === 'correspondence'))
    );
  };

  isLate = () => this.replaying() && status.playing(this.data);

  playerAt = (position: Position) =>
    (this.flip as any) ^ ((position === 'top') as any) ? this.data.opponent : this.data.player;

  flipNow = () => {
    this.flip = !this.nvui && !this.flip;
    this.shogiground.set({
      orientation: ground.boardOrientation(this.data, this.flip),
    });
    this.redraw();
  };

  setTitle = () => title.set(this);

  sendUsi = (data: any, meta: MoveMetadata = {}) => {
    const socketOpts: SocketOpts = {
      ackable: true,
    };
    if (this.clock) {
      socketOpts.withLag = !this.shouldSendMoveTime || !this.clock.isRunning;
      if (meta.premade && this.shouldSendMoveTime) {
        this.clock.hardStopClock();
        socketOpts.millis = 0;
      } else {
        const moveMillis = this.clock.stopClock();
        if (moveMillis !== undefined && this.shouldSendMoveTime) {
          socketOpts.millis = moveMillis;
        }
      }
    }
    this.socket.send('usi', data, socketOpts);

    this.transientMove.register();
    this.redraw();
  };

  sendMove = (orig: sg.Key, dest: sg.Key, prom: boolean, meta: sg.MoveMetadata) => {
    const move: SocketUsi = {
      u: orig + dest,
    };
    if (prom) move.u += '+';
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premade) {
      this.usiToSubmit = move;
      this.redraw();
    } else {
      this.sendUsi(move, meta);
    }
  };

  sendDrop = (role: Role, key: sg.Key, meta: sg.MoveMetadata): void => {
    const drop: SocketUsi = {
      u: roleToString(role).toUpperCase() + '*' + key,
    };
    if (blur.get()) drop.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premade) {
      this.usiToSubmit = drop;
      this.redraw();
    } else {
      this.sendUsi(drop, meta);
    }
  };

  showYourMoveNotification = () => {
    const d = this.data;
    if (game.isPlayerTurn(d))
      notify(() => {
        let txt = this.trans('yourTurn'),
          opponent = renderUser.userTxt(this, d.opponent);
        if (this.ply < 1) txt = opponent + '\njoined the game.\n' + txt;
        else {
          const m_step = d.steps[d.steps.length - 1];
          const prev_step = d.steps[d.steps.length - 2];
          if (m_step && prev_step && m_step.usi) {
            const moveNotation = makeNotation(
              this.data.pref.notation,
              prev_step.sfen,
              this.data.game.variant.key,
              m_step.usi
            );
            txt = opponent + '\nplayed ' + moveNotation + '.\n' + txt;
          }
        }
        return txt;
      });
    else if (this.isPlaying() && this.ply < 1)
      notify(() => {
        return renderUser.userTxt(this, d.opponent) + '\njoined the game.';
      });
  };

  playerByColor = (c: Color) => this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): void => {
    const d = this.data,
      playing = this.isPlaying();

    d.game.plies = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'sente' : 'gote';
    const playedColor: Color = o.ply % 2 === 0 ? 'gote' : 'sente',
      activeColor = d.player.color === d.game.player;
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    this.playerByColor('sente').offeringDraw = o.sDraw;
    this.playerByColor('gote').offeringDraw = o.gDraw;
    this.setTitle();
    const move = parseUsi(o.usi!)!;
    if (!this.replaying()) {
      this.ply++;
      if (isDrop(move)) {
        const capture = this.shogiground.state.pieces.get(makeSquare(move.to) as Key),
          piece = {
            role: move.role,
            color: playedColor,
          };
        // no need to drop the piece if it is already there - would play the sound again
        if (!capture || !samePiece(capture, piece)) this.shogiground.drop(piece, o.usi!.substring(2, 4) as sg.Key);
      } else {
        // This block needs to be idempotent
        const keys = usiToSquareNames(o.usi!) as Key[];
        this.shogiground.move(keys[0], keys[1], move.promotion);
      }
      this.shogiground.set({
        sfen: {
          hands: o.sfen.split(' ')[2],
        },
        turnColor: d.game.player,
        movable: {
          dests: playing && activeColor ? util.getMoveDests(o.sfen, d.game.variant.key) : new Map(),
        },
        droppable: {
          dests: playing && activeColor ? util.getDropDests(o.sfen, d.game.variant.key) : new Map(),
        },
        check: !!o.check,
      });
      if (o.check) sound.check();
      blur.onMove();
      li.pubsub.emit('ply', this.ply);
    }
    const lastStep = round.lastStep(this.data),
      step = {
        ply: lastStep.ply + 1,
        sfen: o.sfen,
        usi: o.usi,
        notation: makeNotation(d.pref.notation, lastStep.sfen, this.data.game.variant.key, o.usi!, lastStep.usi),
        check: o.check,
      };
    d.steps.push(step);
    game.setOnGame(d, playedColor, true);
    this.data.forecastCount = undefined;
    if (o.clock) {
      this.shouldSendMoveTime = true;
      const oc = o.clock,
        delay = playing && activeColor ? 0 : oc.lag || 1;
      if (this.clock) this.clock.setClock(d, oc.sente, oc.gote, oc.sPer, oc.gPer, delay);
      else if (this.corresClock) this.corresClock.update(oc.sente, oc.gote);
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
      setTimeout(() => {
        if (!this.shogiground.playPremove() && !this.shogiground.playPredrop()) {
          this.showYourMoveNotification();
        }
      }, 1);
    }
    this.autoScroll();
    this.onChange();
    if (this.keyboardMove) this.keyboardMove.update(step, playedColor != d.player.color);
    if (this.music) this.music.jump(o);
    speech.step(step);
  };

  reload = (d: RoundData): void => {
    if (d.steps.length !== this.data.steps.length) this.ply = d.steps[d.steps.length - 1].ply;
    round.massage(d);
    this.data = d;
    this.initNotation();
    this.shouldSendMoveTime = false;
    if (this.clock) this.clock.setClock(d, d.clock!.sente, d.clock!.gote, d.clock!.sPeriods, d.clock!.gPeriods);
    if (this.corresClock) this.corresClock.update(d.correspondence.sente, d.correspondence.gote);
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
    // If losing/drawing on time but locally it is the opponent's turn, move did not reach server before the end
    if (
      o.status.name === 'outoftime' &&
      d.player.color !== o.winner &&
      this.shogiground.state.turnColor === d.opponent.color
    ) {
      this.reload(d);
    }
    this.shogiground.stop();
    this.impasseHelp = false;
    if (o.ratingDiff) {
      d.player.ratingDiff = o.ratingDiff[d.player.color];
      d.opponent.ratingDiff = o.ratingDiff[d.opponent.color];
    }
    if (!d.player.spectator && d.game.plies > 1)
      li.sound[o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw']();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock) this.clock.setClock(d, o.clock.sc * 0.01, o.clock.gc * 0.01, o.clock.sp, o.clock.gp);
    this.redraw();
    this.autoScroll();
    this.onChange();
    if (d.tv) setTimeout(li.reload, 10000);
    speech.status(this);
  };

  challengeRematch = (): void => {
    this.challengeRematched = true;
    xhr.challengeRematch(this.data.game.id).then(
      () => {
        li.challengeApp.open();
        if (li.once('rematch-challenge'))
          setTimeout(() => {
            li.hopscotch(function () {
              window.hopscotch
                .configure({
                  i18n: { doneBtn: 'OK, got it' },
                })
                .startTour({
                  id: 'rematch-challenge',
                  showPrevButton: true,
                  steps: [
                    {
                      title: 'Challenged to a rematch',
                      content: 'Your opponent is offline, but they can accept this challenge later!',
                      target: '#challenge-app',
                      placement: 'bottom',
                    },
                  ],
                });
            });
          }, 1000);
      },
      _ => {
        this.challengeRematched = false;
      }
    );
  };

  private makeCorrespondenceClock = (): void => {
    if (this.data.correspondence && !this.corresClock)
      this.corresClock = makeCorresClock(this, this.data.correspondence, this.socket.outoftime);
  };

  private corresClockTick = (): void => {
    if (this.corresClock && game.playable(this.data)) this.corresClock.tick(this.data.game.player);
  };

  private setQuietMode = () => {
    const was = li.quietMode;
    const is = this.isPlaying();
    if (was !== is) {
      li.quietMode = is;
      $('body')
        .toggleClass('playing', is)
        .toggleClass('no-select', is && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5);
    }
  };

  takebackYes = () => {
    this.socket.sendLoading('takeback-yes');
    this.shogiground.cancelPremove();
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
  };

  goBerserk = () => {
    this.socket.berserk();
    li.sound.berserk();
  };

  setBerserk = (color: Color): void => {
    if (this.clock) this.clock.setBerserk(color);
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
    const toSubmit = this.usiToSubmit;
    if (v && this.usiToSubmit) this.sendUsi(this.usiToSubmit);
    else this.jump(this.ply);
    this.cancelMove();
    if (toSubmit) this.setLoading(true, 300);
  };

  cancelMove = (): void => {
    this.usiToSubmit = undefined;
  };

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  private goneTick;
  setGone = (gone: number | boolean) => {
    game.setGone(this.data, this.data.opponent.color, gone);
    clearTimeout(this.goneTick);
    if (Number(gone) > 1)
      this.goneTick = setTimeout(() => {
        const g = Number(this.opponentGone());
        if (g > 1) this.setGone(g - 1);
      }, 1000);
    this.redraw();
  };

  opponentGone = (): number | boolean => {
    const d = this.data;
    return d.opponent.gone !== false && !game.isPlayerTurn(d) && game.resignable(d) && d.opponent.gone;
  };

  canOfferDraw = (): boolean => {
    return game.drawable(this.data) && (this.lastDrawOfferAtPly || -99) < this.ply - 20;
  };

  offerDraw = (v: boolean): void => {
    if (this.canOfferDraw()) {
      if (this.drawConfirm) {
        if (v) this.doOfferDraw();
        clearTimeout(this.drawConfirm);
        this.drawConfirm = undefined;
      } else if (v) {
        if (this.data.pref.confirmResign)
          this.drawConfirm = setTimeout(() => {
            this.offerDraw(false);
          }, 3000);
        else this.doOfferDraw();
      }
    }
    this.redraw();
  };

  private doOfferDraw = () => {
    this.lastDrawOfferAtPly = this.ply;
    this.socket.sendLoading('draw-yes', null);
  };

  setKeyboardMove = () => {
    if (this.data.pref.keyboardMove) {
      this.keyboardMove = makeKeyboardMove(this, this.stepAt(this.ply), this.redraw);
      requestAnimationFrame(() => this.redraw());
    }
  };

  stepAt = (ply: Ply) => round.plyStep(this.data, ply);

  private delayedInit = () => {
    const d = this.data;
    if (this.isPlaying() && game.nbMoves(d, d.player.color) === 0 && !this.isSimulHost()) {
      li.sound.genericNotify();
    }
    li.requestIdleCallback(() => {
      const d = this.data;
      if (this.isPlaying()) {
        if (!d.simul) blur.init(d.steps.length > 2);

        title.init();
        this.setTitle();

        window.addEventListener('beforeunload', e => {
          const d = this.data;
          if (li.hasToReload || this.nvui || !game.playable(d) || !d.clock || d.opponent.ai || this.isSimulHost())
            return;
          this.socket.send('bye2');
          const msg = 'There is a game in progress!';
          (e || window.event).returnValue = msg;
          return msg;
        });

        if (!this.nvui && d.pref.submitMove) {
          window.Mousetrap.bind('esc', () => {
            this.submitMove(false);
            this.shogiground.cancelMove();
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
