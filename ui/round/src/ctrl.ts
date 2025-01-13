import { loadScript } from 'common/assets';
import { requestIdleCallbackWithFallback } from 'common/common';
import notify from 'common/notification';
import * as game from 'game';
import * as status from 'game/status';
import { i18n } from 'i18n';
import { type KeyboardMove, ctrl as makeKeyboardMove } from 'keyboard-move';
import { makeNotation, makeNotationLine } from 'shogi/notation';
import { Shogiground } from 'shogiground';
import type { Api as SgApi } from 'shogiground/api';
import type { Config as SgConfig } from 'shogiground/config';
import type { State } from 'shogiground/state';
import type * as sg from 'shogiground/types';
import { samePiece } from 'shogiground/util';
import { eagleLionAttacks, falconLionAttacks } from 'shogiops/attacks';
import { checksSquareNames, shogigroundSecondLionStep, usiToSquareNames } from 'shogiops/compat';
import { initialSfen, parseSfen } from 'shogiops/sfen';
import type { NormalMove, Piece, Role, Square } from 'shogiops/types';
import {
  defined,
  isDrop,
  makeSquareName,
  makeUsi,
  opposite,
  parseSquareName,
  parseUsi,
  squareDist,
} from 'shogiops/util';
import type { Chushogi } from 'shogiops/variant/chushogi';
import { handRoles, promotableOnDrop, promote, unpromote } from 'shogiops/variant/util';
import * as blur from './blur';
import * as cevalSub from './ceval-sub';
import { ClockController } from './clock/clock-ctrl';
import {
  type CorresClockController,
  ctrl as makeCorresClock,
} from './corres-clock/corres-clock-ctrl';
import * as ground from './ground';
import type {
  ApiEnd,
  ApiMove,
  MoveMetadata,
  NvuiPlugin,
  Position,
  RoundData,
  RoundOpts,
  SocketOpts,
  SocketUsi,
  Step,
} from './interfaces';
import * as keyboard from './keyboard';
import MoveOn from './move-on';
import * as round from './round';
import { type RoundSocket, make as makeSocket } from './socket';
import * as sound from './sound';
import * as speech from './speech';
import * as title from './title';
import TransientMove from './transient-move';
import * as util from './util';
import * as renderUser from './view/user';
import * as xhr from './xhr';

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
  keyboardMove?: KeyboardMove;
  moveOn: MoveOn;

  ply: number;
  firstSeconds = true;
  flip = false;
  loading = false;
  loadingTimeout: number;
  redirecting = false;
  impasseHelp = false;
  postGameStudyOffer = false;
  transientMove: TransientMove;
  usiToSubmit?: SocketUsi;
  lionFirstMove?: NormalMove;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  pauseConfirm?: Timeout = undefined;
  // will be replaced by view layer
  autoScroll: () => void = $.noop;
  challengeRematched = false;
  shouldSendMoveTime = false;
  openStudyModal = false;
  lastDrawOfferAtPly?: Ply;
  lastPauseOfferAtTime?: number;
  nvui?: NvuiPlugin;

  private music?: any;

  constructor(
    readonly opts: RoundOpts,
    readonly redraw: Redraw,
  ) {
    round.massage(opts.data);

    this.data = opts.data;
    const d = this.data;

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

    if (li.modules.roundNvui) this.nvui = li.modules.roundNvui(redraw) as NvuiPlugin;

    if (d.clock)
      this.clock = new ClockController(d, {
        onFlag: this.socket.outoftime,
        redraw: this.redraw,
        soundColor:
          d.simul || d.player.spectator || !d.pref.clockSound ? undefined : d.player.color,
        nvui: !!this.nvui,
      });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'move-on');
    this.transientMove = new TransientMove(this.socket);

    this.setKeyboardMove();

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer || document.referrer.indexOf('/service-worker.js') === -1)
      setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    li.pubsub.on('jump', ply => {
      this.jump(Number.parseInt(ply));
      this.redraw();
    });

    li.pubsub.on('sound_set', set => {
      if (!this.music && set === 'music')
        loadScript('javascripts/music/play.js').then(() => {
          this.music = li.modules.miscMusic();
        });
      if (this.music && set !== 'music') this.music = undefined;
    });

    li.pubsub.on('zen', () => {
      if (this.isPlaying()) {
        const zen = !$('body').hasClass('zen');
        $('body').toggleClass('zen', zen);
        window.dispatchEvent(new Event('resize'));
        xhr.setZen(zen);
      }
    });
  }

  private initNotation = (): void => {
    if (this.data.steps.length <= 1) return;
    const usis = this.data.steps.slice(1).map(s => s.usi!);
    const movesNotation: MoveNotation[] = makeNotationLine(
      this.data.game.initialSfen || initialSfen(this.data.game.variant.key),
      this.data.game.variant.key,
      usis,
    );
    for (let i = 1; i < this.data.steps.length; i++)
      this.data.steps[i].notation = movesNotation[i - 1];
  };

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  };

  private onUserMove = (orig: Key, dest: Key, prom: boolean, meta: sg.MoveMetadata) => {
    if (this.data.game.variant.key === 'chushogi')
      return this.onChushogiMove(orig, dest, prom, meta);

    // to update hand immediately and not wait on apiMove
    if (meta.captured) {
      const role = meta.captured.role as Role;
      const unpromotedRole = handRoles(this.data.game.variant.key).includes(role)
        ? role
        : unpromote(this.data.game.variant.key)(role) || role;
      this.shogiground.addToHand({
        role: unpromotedRole,
        color: opposite(meta.captured.color),
      });
    }
    const usi = orig + dest + (prom ? '+' : '');
    this.sendUsi(usi, meta);
  };

  private onUserDrop = (piece: Piece, key: Key, prom: boolean, meta: sg.MoveMetadata) => {
    let role = piece.role;
    if (prom && promotableOnDrop(this.data.game.variant.key)(piece))
      role = promote(this.data.game.variant.key)(role) || role;
    const usi = makeUsi({ role: role, to: parseSquareName(key) });
    this.sendUsi(usi, meta);
  };

  private onChushogiMove = (orig: Key, dest: Key, prom: boolean, meta: sg.MoveMetadata) => {
    const posRes = parseSfen(this.data.game.variant.key, this.stepAt(this.ply).sfen, false);
    const piece = posRes.isOk && posRes.value.board.get(parseSquareName(orig))!;
    if (
      piece &&
      this.lionFirstMove === undefined &&
      squareDist(parseSquareName(orig), parseSquareName(dest)) === 1 &&
      (['lion', 'lionpromoted'].includes(piece.role) ||
        (piece.role === 'eagle' &&
          eagleLionAttacks(parseSquareName(orig), piece.color).has(parseSquareName(dest))) ||
        (piece.role === 'falcon' &&
          falconLionAttacks(parseSquareName(orig), piece.color).has(parseSquareName(dest))))
    ) {
      const pos = posRes.value as Chushogi;
      this.shogiground.set({
        activeColor: pos.turn,
        turnColor: pos.turn,
        checks: checksSquareNames(pos),
        movable: {
          dests: shogigroundSecondLionStep(pos, orig, dest),
        },
        drawable: {
          squares: [{ key: dest, className: 'force-selected' }],
        },
      });
      this.shogiground.selectSquare(dest, false, true);
      this.lionFirstMove = {
        from: parseSquareName(orig),
        to: parseSquareName(dest),
      };
    } else {
      const hadMid =
        this.lionFirstMove !== undefined && makeSquareName(this.lionFirstMove.to) !== dest;
      const move: NormalMove = {
        from: hadMid ? this.lionFirstMove!.from : parseSquareName(orig),
        to: parseSquareName(dest),
        promotion: prom,
        midStep: hadMid ? this.lionFirstMove!.to : undefined,
      };
      this.lionFirstMove = undefined;
      const usi = makeUsi(move);
      this.sendUsi(usi, meta);
    }
  };

  private onMove = (_orig: Key, _dest: Key, _prom: boolean, captured?: Piece) => {
    if (status.prepaused(this.data)) return;
    if (captured) {
      sound.capture();
    } else sound.move();
  };

  private onDrop = (_piece: Piece, _key: Key, _prom: boolean) => {
    if (status.prepaused(this.data)) return;
    sound.move();
  };

  private isSimulHost = () => {
    return this.data.simul && this.data.simul.hostId === this.opts.userId;
  };

  makeSgHooks = (): {
    onUserMove: NonNullable<State['movable']['events']['after']>;
    onUserDrop: NonNullable<State['droppable']['events']['after']>;
    onMove: NonNullable<State['events']['move']>;
    onDrop: NonNullable<State['events']['drop']>;
  } => ({
    onUserMove: this.onUserMove,
    onUserDrop: this.onUserDrop,
    onMove: this.onMove,
    onDrop: this.onDrop,
  });

  replaying = (): boolean => this.ply !== round.lastPly(this.data);

  userJump = (ply: Ply): void => {
    this.cancelMoveOrDrop();
    this.lionFirstMove = undefined;
    this.shogiground.selectSquare(null);
    if (ply != this.ply && this.jump(ply)) speech.userJump(this, this.ply);
    else this.redraw();
  };

  userJumpPlyDelta = (plyDelta: Ply): void => this.userJump(this.ply + plyDelta);

  isPlaying = (): boolean => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(round.firstPly(this.data), Math.min(round.lastPly(this.data), ply));
    const isForwardStep = ply === this.ply + 1;
    this.ply = ply;
    const s = this.stepAt(ply);
    const splitSfen = s.sfen.split(' ');
    const variant = this.data.game.variant.key;
    const posRes = this.isPlaying() ? parseSfen(variant, s.sfen, false) : undefined;
    const config: SgConfig = {
      sfen: { board: splitSfen[0], hands: splitSfen[2] },
      lastDests: s.usi ? usiToSquareNames(s.usi) : undefined,
      checks: !!s.check,
      turnColor: this.ply % 2 === 0 ? 'sente' : 'gote',
      activeColor: this.isPlaying() ? this.data.player.color : undefined,
      drawable: {
        squares: [],
      },
    };
    if (this.replaying()) this.shogiground.stop();
    else {
      config.movable = {
        dests: this.isPlaying() && posRes ? util.getMoveDests(posRes) : new Map(),
      };
      config.droppable = {
        dests: this.isPlaying() && posRes ? util.getDropDests(posRes) : new Map(),
      };
    }
    const move = s.usi && parseUsi(s.usi);
    const capture =
      isForwardStep && move && this.shogiground.state.pieces.get(makeSquareName(move.to));
    this.shogiground.set(config);
    if (s.usi && isForwardStep) {
      if (capture) sound.capture();
      else sound.move();
      if (s.check && variant !== 'chushogi') sound.check();
    }
    this.lionFirstMove = undefined;
    this.autoScroll();
    if (this.keyboardMove)
      this.keyboardMove.update({ sfen: s.sfen, lastSquare: this.stepDest(this.stepAt(ply)) });
    li.pubsub.emit('ply', ply);
    return true;
  };

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return (
      d.pref.replay === 2 ||
      (d.pref.replay === 1 &&
        (d.game.speed === 'classical' ||
          d.game.speed === 'unlimited' ||
          d.game.speed === 'correspondence'))
    );
  };

  isLate = (): boolean => this.replaying() && status.playing(this.data);

  playerAt = (position: Position): game.Player =>
    (this.flip as any) ^ ((position === 'top') as any) ? this.data.opponent : this.data.player;

  flipNow = (): void => {
    this.flip = !this.nvui && !this.flip;
    this.shogiground.set({
      orientation: ground.boardOrientation(this.data, this.flip),
    });
    this.redraw();
  };

  setTitle = (): void => title.set(this);

  sendUsiData = (data: any, meta: MoveMetadata = {}): void => {
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

    if (!status.prepaused(this.data))
      this.transientMove.register(this.clock?.times[this.data.player.color]);
    this.redraw();
  };

  sendUsi = (usi: Usi, meta: sg.MoveMetadata): void => {
    const socUsi: SocketUsi = {
      u: usi,
    };
    if (blur.get()) socUsi.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && !meta.premade) {
      this.usiToSubmit = socUsi;
      this.redraw();
    } else {
      this.sendUsiData(socUsi, meta);
    }
  };

  showYourMoveNotification = (): void => {
    const d = this.data;
    if (game.isPlayerTurn(d))
      notify(() => {
        let txt = i18n('yourTurn');
        const opponent = renderUser.userTxt(d.opponent);
        if (this.ply < 1) txt = `${opponent}\njoined the game.\n${txt}`;
        else {
          const m_step = d.steps[d.steps.length - 1];
          const prev_step = d.steps[d.steps.length - 2];
          if (m_step && prev_step && m_step.usi) {
            const moveNotation = makeNotation(
              prev_step.sfen,
              this.data.game.variant.key,
              m_step.usi,
            );
            txt = `${opponent}\nplayed ${moveNotation}.\n${txt}`;
          }
        }
        return txt;
      });
    else if (this.isPlaying() && this.ply < 1)
      notify(() => {
        return `${renderUser.userTxt(d.opponent)}\njoined the game.`;
      });
  };

  playerByColor = (c: Color): game.Player =>
    this.data[c === this.data.player.color ? 'player' : 'opponent'];

  apiMove = (o: ApiMove): void => {
    const d = this.data;
    const playing = this.isPlaying();

    d.game.plies = o.ply;
    d.game.player = o.ply % 2 === 0 ? 'sente' : 'gote';
    const playedColor: Color = o.ply % 2 === 0 ? 'gote' : 'sente';
    const activeColor = d.player.color === d.game.player;
    const variant = d.game.variant.key;
    if (o.status) d.game.status = o.status;
    if (o.winner) d.game.winner = o.winner;
    this.playerByColor('sente').offeringDraw = o.sDraw;
    this.playerByColor('gote').offeringDraw = o.gDraw;
    this.setTitle();
    const move = parseUsi(o.usi!)!;
    const keys = usiToSquareNames(o.usi!);
    const posRes =
      playing && activeColor ? parseSfen(d.game.variant.key, o.sfen, false) : undefined;

    if (!this.replaying()) {
      this.ply++;
      if (isDrop(move)) {
        const capture = this.shogiground.state.pieces.get(keys[0]);
        const unpromotedRole =
          variant === 'kyotoshogi' && !handRoles(variant).includes(move.role)
            ? unpromote(variant)(move.role)
            : undefined; // unify this behaviour, somewhere we get piece already promoted, somwehere we get original piece with boolean promotion
        const piece = {
          role: unpromotedRole || move.role,
          color: playedColor,
        };
        // no need to drop the piece if it is already there - would play the sound again
        if (!capture || !samePiece(capture, piece))
          this.shogiground.drop(piece, keys[0], !!unpromotedRole);
      } else {
        // This block needs to be idempotent
        if (defined(move.midStep)) {
          const orig = keys[0];
          const midStep = keys[1];
          const dest = keys[2];
          if (orig !== dest) this.shogiground.move(orig, dest, move.promotion);
          else if (game.isPlayerTurn(d)) {
            const capture = this.shogiground.state.pieces.get(midStep);
            this.onMove(orig, dest, !!move.promotion, capture as Piece);
          }
          this.shogiground.state.pieces.delete(midStep);
        } else this.shogiground.move(keys[0], keys[1], move.promotion);
      }
      this.shogiground.set({
        sfen: {
          hands: o.sfen.split(' ')[2],
        },
        turnColor: d.game.player,
        lastDests: keys,
        movable: {
          dests: playing && activeColor && posRes ? util.getMoveDests(posRes) : new Map(),
        },
        droppable: {
          dests: playing && activeColor && posRes ? util.getDropDests(posRes) : new Map(),
        },
        checks: o.check,
        drawable: {
          squares: [],
        },
      });
      if (o.check && variant !== 'chushogi') sound.check();
      blur.onMove();
      li.pubsub.emit('ply', this.ply);
    }
    const lastStep = round.lastStep(d);
    const step = {
      ply: lastStep.ply + 1,
      sfen: o.sfen,
      usi: o.usi,
      notation: makeNotation(lastStep.sfen, d.game.variant.key, o.usi!, lastStep.usi),
      check: o.check,
    };
    d.steps.push(step);
    game.setOnGame(d, playedColor, true);
    this.data.forecastCount = undefined;
    if (o.clock) {
      this.shouldSendMoveTime = true;
      const oc = o.clock;
      const delay = playing && activeColor ? 0 : oc.lag || 1;
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
    if (this.keyboardMove)
      this.keyboardMove.update({
        sfen: step.sfen,
        lastSquare: lastStep?.usi ? parseUsi(lastStep.usi)?.to : undefined,
      });
    if (this.music) this.music.jump(o);
    speech.notation(step.notation);
  };

  reload = (d: RoundData): void => {
    if (d.steps.length !== this.data.steps.length) this.ply = d.steps[d.steps.length - 1].ply;
    round.massage(d);
    this.data = d;
    this.initNotation();
    this.shouldSendMoveTime = false;
    this.lionFirstMove = undefined;
    if (this.clock)
      this.clock.setClock(d, d.clock!.sente, d.clock!.gote, d.clock!.sPeriods, d.clock!.gPeriods);
    if (this.corresClock) this.corresClock.update(d.correspondence.sente, d.correspondence.gote);
    if (!this.replaying()) ground.reload(this);
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.redraw();
    this.autoScroll();
    this.onChange();
    this.setLoading(false);
    if (this.keyboardMove)
      this.keyboardMove.update({
        sfen: d.steps[d.steps.length - 1].sfen,
        lastSquare: this.stepDest(d.steps[d.steps.length - 2]),
      });
  };

  endWithData = (o: ApiEnd): void => {
    console.log('endWithData ctrl'); // todo
    const d = this.data;
    d.game.winner = o.winner;
    d.game.status = o.status;
    d.game.boosted = o.boosted;
    this.userJump(round.lastPly(d));
    // If losing/drawing on time but locally it is the opponent's turn, move did not reach server before the end
    if (
      o.status.id === status.ids.outoftime &&
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
      li.sound.play(o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw');
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock)
      this.clock.setClock(d, o.clock.sc * 0.01, o.clock.gc * 0.01, o.clock.sp, o.clock.gp);
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
        li.challengeApp?.open();
      },
      _ => {
        this.challengeRematched = false;
      },
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
        .toggleClass(
          'no-select',
          is && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5,
        );
    }
  };

  takebackYes = (): void => {
    this.socket.sendLoading('takeback-yes');
    this.shogiground.cancelPremove();
  };

  resign = (v: boolean, force = false): void => {
    if (v) {
      if (this.resignConfirm || !this.data.pref.confirmResign || force) {
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

  goBerserk = (): void => {
    this.socket.berserk();
    li.sound.play('berserk');
  };

  setBerserk = (color: Color): void => {
    if (this.clock) this.clock.setBerserk(color);
    if (this.goneBerserk[color]) return;
    this.goneBerserk[color] = true;
    if (color !== this.data.player.color) li.sound.play('berserk');
    this.redraw();
    $('<i data-icon="`">').appendTo($(`.game__meta .player.${color} .user-link`));
  };

  setLoading = (v: boolean, duration = 1500): void => {
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

  setRedirecting = (): void => {
    this.redirecting = true;
    setTimeout(() => {
      this.redirecting = false;
      this.redraw();
    }, 2500);
    this.redraw();
  };

  submitUsi = (v: boolean): void => {
    const toSubmit = this.usiToSubmit;
    if (v && this.usiToSubmit) this.sendUsiData(this.usiToSubmit);
    else this.jump(this.ply);
    this.cancelMoveOrDrop();
    if (toSubmit) this.setLoading(true, 300);
  };

  cancelMoveOrDrop = (): void => {
    this.usiToSubmit = undefined;
  };

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  private goneTick: number | undefined;
  setGone = (gone: number | boolean): void => {
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
    return (
      d.opponent.gone !== false && !game.isPlayerTurn(d) && game.resignable(d) && d.opponent.gone
    );
  };

  showDrawButton = (): boolean => {
    return this.data.game.variant.key === 'chushogi';
  };

  canOfferDraw = (): boolean => {
    return (
      game.drawable(this.data) &&
      (!this.lastDrawOfferAtPly || this.lastDrawOfferAtPly < this.ply - 20)
    );
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

  showPauseButton = (): boolean => {
    return (
      this.data.game.variant.key === 'chushogi' &&
      !!this.data.opponent.user &&
      !!document.body.dataset.user &&
      ((this.data.clock?.initial || 0) >= 60 * 15 || (this.data.clock?.byoyomi || 0) >= 30)
    );
  };

  canOfferPause = (): boolean => {
    return (
      game.pausable(this.data) &&
      (!this.lastPauseOfferAtTime || Date.now() - this.lastPauseOfferAtTime > 10 * 60 * 1000)
    );
  };

  offerPause = (v: boolean): void => {
    if (this.canOfferPause()) {
      if (this.pauseConfirm) {
        if (v) this.doOfferPause();
        clearTimeout(this.pauseConfirm);
        this.pauseConfirm = undefined;
      } else if (v) {
        if (this.data.pref.confirmResign)
          this.pauseConfirm = setTimeout(() => {
            this.offerPause(false);
          }, 3500);
        else this.doOfferPause();
      }
    }
    this.redraw();
  };

  private doOfferPause = () => {
    this.lastPauseOfferAtTime = Date.now();
    this.socket.sendLoading('pause-yes', null);
  };

  showImpasseButton = (): boolean => {
    return !this.showDrawButton();
  };

  private setKeyboardMove = () => {
    if (this.data.pref.keyboardMove && this.data.game.variant.key !== 'chushogi') {
      this.keyboardMove = makeKeyboardMove(this, {
        sfen: this.stepAt(this.ply).sfen,
        lastSquare: this.stepDest(this.stepAt(this.ply - 1)),
      });
      requestAnimationFrame(() => this.redraw());
    }
  };

  stepAt = (ply: Ply): Step => round.plyStep(this.data, ply);

  private stepDest = (step: Step | undefined): Square | undefined => {
    if (step?.usi) return parseUsi(step.usi)?.to;
    else return;
  };

  private delayedInit = () => {
    const d = this.data;
    if (this.isPlaying() && game.nbMoves(d, d.player.color) === 0 && !this.isSimulHost()) {
      li.sound.play('genericNotify');
    }
    requestIdleCallbackWithFallback(() => {
      const d = this.data;
      if (this.isPlaying()) {
        if (!d.simul) blur.init(d.steps.length > 2);

        title.init();
        this.setTitle();

        window.addEventListener('beforeunload', e => {
          const d = this.data;
          if (
            window.lishogi.properReload ||
            this.nvui ||
            !game.playable(d) ||
            !d.clock ||
            game.hasAi(d) ||
            this.isSimulHost()
          )
            return;
          this.socket.send('bye2');
          const msg = 'There is a game in progress!';
          (e || window.event).returnValue = msg;
          return msg;
        });

        if (!this.nvui && d.pref.submitMove) {
          window.lishogi.mousetrap.bind('esc', () => {
            this.submitUsi(false);
            this.shogiground.cancelMoveOrDrop();
          });
          window.lishogi.mousetrap.bind('return', () => this.submitUsi(true));
        }
        cevalSub.subscribe(this);
      }

      if (!this.nvui) keyboard.init(this);

      speech.setup(this);

      this.onChange();
    });
  };
}
