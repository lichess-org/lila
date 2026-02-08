/// <reference types="../types/ab" />

import * as ab from 'ab';
import * as game from 'lib/game';
import { game as gameRoute } from 'lib/game/router';
import { playing } from 'lib/game/status';
import { boardOrientation, reload as groundReload } from './ground';
import * as licon from 'lib/licon';
import notify from 'lib/notification';
import { make as makeSocket, type RoundSocket } from './socket';
import * as title from './title';
import * as blur from './blur';
import viewStatus from 'lib/game/view/status';
import { ClockCtrl, type ClockOpts } from 'lib/game/clock/clockCtrl';
import { CorresClockController } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import TransientMove from './transientMove';
import * as atomic from './atomic';
import * as util from './util';
import * as xhr from './xhr';
import { valid as crazyValid, init as crazyInit, onEnd as crazyEndHook } from './crazy/crazyCtrl';
import type { MoveRootCtrl } from 'lib/game/moveRootCtrl';
import { ctrl as makeKeyboardMove, type KeyboardMove } from 'keyboardMove';
import { makeVoiceMove, type VoiceMove } from 'voice';
import { userTxt } from './view/user';
import * as cevalSub from './cevalSub';
import { init as keyboardInit } from './keyboard';
import { PromotionCtrl, promote } from 'lib/game/promotion';
import * as wakeLock from 'lib/wakeLock';
import { opposite, uciToMove } from '@lichess-org/chessground/util';
import { Replay } from 'lib/prefs';
import { endGameView } from './view/main';
import type {
  Step,
  RoundOpts,
  RoundData,
  SocketMove,
  SocketDrop,
  MoveMetadata,
  NvuiPlugin,
  RoundTour,
  ApiMove,
  ApiEnd,
  EventsWithPayload,
} from './interfaces';
import { defined, type Toggle, type Prop, toggle, requestIdleCallback, memoize } from 'lib';
import { storage, once, storedBooleanProp, type LichessBooleanStorage } from 'lib/storage';
import * as poolRangeStorage from 'lib/poolRangeStorage';
import { pubsub } from 'lib/pubsub';
import { readFen, almostSanOf, speakable } from 'lib/game/sanWriter';
import { plyToTurn } from 'lib/game/chess';
import { type SocketSendOpts } from 'lib/socket';
import type { NodeCrazy } from 'lib/tree/types';
import Server from './server';
import { bindBookmarkButton } from 'lib/game/bookmark';

type GoneBerserk = Partial<ByColor<boolean>>;

export default class RoundController implements MoveRootCtrl {
  data: RoundData;
  socket: RoundSocket;
  chessground: CgApi;
  clock?: ClockCtrl;
  corresClock?: CorresClockController;
  keyboardMove?: KeyboardMove;
  voiceMove?: VoiceMove;
  moveOn: MoveOn;
  promotion: PromotionCtrl;
  ply: number;
  firstSeconds = true;
  flip = false;
  menu: Toggle;
  confirmMoveToggle: Toggle;
  loading = false;
  loadingTimeout: number;
  redirecting = false;
  transientMove?: TransientMove;
  toSubmit?: SocketMove | SocketDrop;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  preventDrawOffer?: Timeout = undefined;
  // will be replaced by view layer
  autoScroll: () => void = () => {};
  justDropped?: Role;
  justCaptured?: Piece;
  shouldSendMoveTime = false;
  preDrop?: Role;
  sign: string = Math.random().toString(36);
  keyboardHelp: boolean = location.hash === '#keyboard';
  blindfoldStorage: LichessBooleanStorage;
  server: Server;
  nvui?: NvuiPlugin;
  vibration: Prop<boolean> = storedBooleanProp('vibration', false);

  constructor(
    readonly opts: RoundOpts,
    readonly redraw: Redraw,
  ) {
    util.upgradeServerData(opts.data);

    const d = (this.data = opts.data);

    this.ply = util.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;
    setTimeout(() => {
      this.firstSeconds = false;
      this.redraw();
    }, 3000);
    this.socket = d.local ?? makeSocket(opts.socketSend!, this);
    this.blindfoldStorage = storage.boolean(`blindfold.${this.data.player.user?.id ?? 'anon'}`);

    this.updateClockCtrl();
    this.promotion = new PromotionCtrl(
      f => f(this.chessground),
      () => {
        this.chessground.cancelPremove();
        xhr.reload(this.data).then(this.reload, site.reload);
      },
      this.redraw,
      d.pref.autoQueen,
    );

    this.setQuietMode();
    this.confirmMoveToggle = toggle(d.pref.submitMove);
    this.moveOn = new MoveOn(this, 'move-on');
    if (!d.local) this.transientMove = new TransientMove(this.socket);
    this.server = new Server(() => this.data);

    this.menu = toggle(false, redraw);
    const nvuiPromise = site.blindMode && site.asset.loadEsm<NvuiPlugin>('round.nvui', { init: this });
    setTimeout(async () => {
      if (nvuiPromise) this.nvui = await nvuiPromise;
      this.delayedInit();
    }, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer?.includes('/serviceWorker.')) setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    pubsub.on('jump', ply => {
      this.jump(parseInt(ply));
      this.redraw();
    });

    pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      if (!$('body').hasClass('zen-auto')) {
        xhr.setZen(zen);
      }
    });

    if (!this.opts.noab && this.isPlaying()) ab.init(this);
  }

  private showExpiration = () => {
    if (!this.data.expiration) return;
    this.redraw();
    setTimeout(this.showExpiration, 250);
  };

  private onUserMove = (orig: Key, dest: Key, meta: MoveMetadata) => {
    if (!this.keyboardMove?.usedSan) ab.move(this, meta, pubsub.emit);
    if (!this.startPromotion(orig, dest, meta)) this.sendMove(orig, dest, undefined, meta);
  };

  private onUserNewPiece = (role: Role, key: Key, meta: MoveMetadata) => {
    if (!this.replaying() && crazyValid(this.data, role, key)) {
      this.sendNewPiece(role, key, !!meta.predrop);
    } else this.jump(this.ply);
  };

  private onMove = (orig: Key, dest: Key, captured?: Piece) => {
    if (captured || this.enpassant(orig, dest)) {
      if (this.data.game.variant.key === 'atomic') {
        site.sound.play('explosion');
        atomic.capture(this, dest);
      } else site.sound.move({ name: 'capture', filter: 'game' });
    } else site.sound.move({ name: 'move', filter: 'game' });
  };

  private startPromotion = (orig: Key, dest: Key, meta: MoveMetadata) =>
    this.promotion.start(
      orig,
      dest,
      {
        submit: (orig, dest, role) => this.sendMove(orig, dest, role, meta),
        show: this.voiceMove?.promotionHook(),
      },
      meta,
      this.keyboardMove?.justSelected(),
    );

  private onPremove = (orig: Key, dest: Key, meta: MoveMetadata) => this.startPromotion(orig, dest, meta);

  private onCancelPremove = () => this.promotion.cancelPrePromotion();

  private onNewPiece = (piece: Piece, key: Key): void => {
    if (piece.role === 'pawn' && (key[1] === '1' || key[1] === '8')) return;
    site.sound.move();
  };

  private onPredrop = (role: Role | undefined, _?: Key) => {
    this.preDrop = role;
    this.redraw();
  };

  private isSimulHost = () => this.data.simul && this.data.simul.hostId === this.opts.userId;

  private enpassant = (orig: Key, dest: Key): boolean => {
    if (orig[0] === dest[0] || this.chessground.state.pieces.get(dest)?.role !== 'pawn') return false;
    const pos = (dest[0] + orig[1]) as Key;
    this.chessground.setPieces(new Map([[pos, undefined]]));
    return true;
  };

  lastPly = (): number => util.lastPly(this.data);

  makeCgHooks = (): any => ({
    onUserMove: this.onUserMove,
    onUserNewPiece: this.onUserNewPiece,
    onMove: this.data.local ? undefined : this.onMove,
    onNewPiece: this.onNewPiece,
    onPremove: this.onPremove,
    onCancelPremove: this.onCancelPremove,
    onPredrop: this.onPredrop,
  });

  replaying = (): boolean => this.ply !== this.lastPly() && !this.data.local;

  userJump = (ply: Ply): void => {
    this.toSubmit = undefined;
    this.chessground.selectSquare(null);
    if (ply !== this.ply && this.jump(ply)) site.sound.saySan(this.stepAt(this.ply).san, true);
    else this.redraw();
  };

  userJumpPlyDelta = (plyDelta: Ply): void => this.userJump(this.ply + plyDelta);

  isPlaying = (): boolean => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(util.firstPly(this.data), Math.min(this.lastPly(), ply));
    const isForwardStep = ply === this.ply + 1;
    this.ply = ply;
    this.justDropped = undefined;
    this.preDrop = undefined;
    const s = this.stepAt(ply),
      config: CgConfig = {
        fen: s.fen,
        lastMove: uciToMove(s.uci),
        check: !!s.check,
        turnColor: this.ply % 2 === 0 ? 'white' : 'black',
      };
    if (this.replaying()) this.chessground.stop();
    else
      config.movable = {
        color: this.isPlaying() ? this.data.player.color : undefined,
        dests: util.parsePossibleMoves(this.data.possibleMoves),
      };
    this.chessground.cancelPremove();
    this.chessground.set(config);
    if (s.san && isForwardStep) site.sound.move(s);
    this.autoScroll();
    pubsub.emit('ply', ply);
    this.pluginUpdate(s.fen);
    return true;
  };

  canMove = (): boolean => !this.replaying() && this.data.player.color === this.chessground.state.turnColor;

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return (
      d.pref.replay === Replay.Always ||
      (d.pref.replay === Replay.OnlySlowGames &&
        (d.game.speed === 'classical' || d.game.speed === 'correspondence'))
    );
  };

  isLate = (): boolean => this.replaying() && playing(this.data);

  playerAt = (position: game.TopOrBottom): game.Player =>
    this.flip !== (position === 'top') ? this.data.opponent : this.data.player;

  flipNow = (): void => {
    this.flip = !this.nvui && !this.flip;
    this.chessground.set({
      orientation: boardOrientation(this.data, this.flip),
    });
    pubsub.emit('flip', this.flip);
    this.redraw();
  };

  setTitle = (): void => title.set(this);

  actualSendMove = <moveOrDrop extends 'move' | 'drop'>(
    tpe: moveOrDrop,
    data: EventsWithPayload[moveOrDrop],
    meta: MoveMetadata = { premove: false },
  ): void => {
    const socketOpts: SocketSendOpts = {
      sign: this.sign,
      ackable: true,
    };
    if (this.clock) {
      socketOpts.withLag = !this.shouldSendMoveTime || !this.clock.isRunning();
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
    this.transientMove?.register();
    this.redraw();
  };

  pluginMove = (orig: Key, dest: Key, role?: Role, preConfirmed?: boolean): void => {
    if (!role) {
      this.chessground.move(orig, dest);
      this.chessground.state.movable.dests = undefined;
      this.chessground.state.turnColor = opposite(this.chessground.state.turnColor);

      if (this.startPromotion(orig, dest, { premove: false })) return;
    }
    this.sendMove(orig, dest, role, { premove: false, preConfirmed });
  };

  pluginUpdate = (fen: string): void => {
    this.voiceMove?.update({ fen, canMove: this.canMove() });
    this.keyboardMove?.update({ fen, canMove: this.canMove() });
  };

  sendMove = (orig: Key, dest: Key, prom: Role | undefined, meta: MoveMetadata): void => {
    const move: SocketMove = { u: orig + dest };
    if (prom) move.u += prom === 'knight' ? 'n' : prom[0];
    if (blur.get()) move.b = 1;
    this.resign(false);

    if (!meta.preConfirmed && this.confirmMoveToggle() && !meta.premove) {
      if (site.sound.speech()) {
        const spoken = `${speakable(almostSanOf(readFen(this.stepAt(this.ply).fen), move.u))}. confirm?`;
        site.sound.say(spoken, false, true);
      }
      this.toSubmit = move;
      this.redraw();
      return;
    }
    this.actualSendMove('move', move, { justCaptured: meta.captured, premove: meta.premove });
  };

  sendNewPiece = (role: Role, key: Key, isPredrop: boolean): void => {
    const drop: SocketDrop = { role, pos: key };
    if (blur.get()) drop.b = 1;
    this.resign(false);
    if (this.confirmMoveToggle() && !isPredrop) {
      this.toSubmit = drop;
      this.redraw();
    } else {
      this.actualSendMove('drop', drop, {
        justDropped: role,
        premove: isPredrop,
      });
    }
  };

  showYourMoveNotification = (): void => {
    if (this.data.local) return;
    const d = this.data;
    const opponent = $('body').hasClass('zen') ? 'Your opponent' : userTxt(d.opponent);
    const joined = `${opponent}\njoined the game.`;
    if (game.isPlayerTurn(d))
      notify(() => {
        let txt = i18n.site.yourTurn;
        if (this.ply < 1) txt = `${joined}\n${txt}`;
        else {
          let move = d.steps[d.steps.length - 1].san;
          const turn = plyToTurn(this.ply);
          move = `${turn}${this.ply % 2 === 1 ? '.' : '...'} ${move}`;
          txt = `${opponent}\nplayed ${move}.\n${txt}`;
        }
        return txt;
      });
    else if (this.isPlaying() && this.ply < 1) notify(joined);
  };

  playerByColor = (c: Color): game.Player => this.data[c === this.data.player.color ? 'player' : 'opponent'];

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
      if (o.role)
        this.chessground.newPiece(
          {
            role: o.role,
            color: playedColor,
          },
          o.uci.slice(2, 4) as Key,
        );
      else {
        // This block needs to be idempotent, even for castling moves in
        // Chess960.
        const keys = uciToMove(o.uci)!,
          pieces = this.chessground.state.pieces;
        if (
          !o.castle ||
          (pieces.get(o.castle.king[0])?.role === 'king' && pieces.get(o.castle.rook[0])?.role === 'rook')
        ) {
          this.chessground.move(keys[0], keys[1]);
        }
      }
      if (o.promotion) promote(this.chessground, o.promotion.key, o.promotion.pieceClass);
      this.chessground.set({
        turnColor: d.game.player,
        movable: {
          dests: playing ? util.parsePossibleMoves(d.possibleMoves) : new Map(),
        },
        check: !!o.check,
      });
      if (o.status?.name === 'mate') {
        site.sound.play('checkmate', o.volume);
      } else if (o.check) {
        site.sound.play('check', o.volume);
      }
      blur.onMove();
      pubsub.emit('ply', this.ply);
    }
    d.game.threefold = !!o.threefold;
    d.game.fiftyMoves = !!o.fiftyMoves;
    const step = {
      ply: this.lastPly() + 1,
      fen: o.fen,
      san: o.san,
      uci: o.uci,
      check: o.check,
      crazy: o.crazyhouse,
    };
    d.steps.push(step);
    this.justDropped = undefined;
    this.justCaptured = undefined;
    game.setOnGame(d, playedColor, true);
    this.data.forecastCount = undefined;
    if (o.clock) {
      this.shouldSendMoveTime = true;
      const oc = o.clock,
        delay = playing && activeColor ? 0 : oc.lag || 1;
      if (this.clock)
        this.clock.setClock({
          white: oc.white,
          black: oc.black,
          ticking: this.tickingClockColor(),
          delay,
        });
      else if (this.corresClock) this.corresClock.update(oc.white, oc.black);
    }
    if (this.data.expiration) {
      if (this.data.steps.length > 2) this.data.expiration = undefined;
      else this.data.expiration.movedAt = Date.now();
    }
    this.redraw();
    if (playing && playedColor === d.player.color) {
      this.transientMove?.clear();
      this.moveOn.next();
      cevalSub.publish(d, o);
    }
    if (!this.replaying() && playedColor !== d.player.color) {
      if (this.vibration() && 'vibrate' in navigator) navigator.vibrate(100);
      // prevent race conditions with explosions and premoves
      // https://github.com/lichess-org/lila/issues/343
      const premoveDelay = d.game.variant.key === 'atomic' ? 100 : 1;
      setTimeout(() => {
        if (this.nvui) this.nvui.playPremove();
        else if (!this.chessground.playPremove() && !this.playPredrop()) {
          this.promotion.cancel();
          this.showYourMoveNotification();
        }
      }, premoveDelay);
    }
    this.autoScroll();
    this.onChange();
    this.pluginUpdate(step.fen);
    if (!this.data.local) site.sound.move({ ...o, filter: 'music' });
    site.sound.saySan(step.san);
    this.server.alive();
    return true; // prevents default socket pubsub
  };

  crazyValid = (role: Role, key: Key): boolean => crazyValid(this.data, role, key);

  getCrazyhousePockets = (): NodeCrazy['pockets'] | undefined => this.data.crazyhouse?.pockets;

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
    util.upgradeServerData(d);
    this.data = d;
    this.clearJust();
    this.shouldSendMoveTime = false;
    this.updateClockCtrl();
    if (this.clock)
      this.clock.setClock({
        white: d.clock!.white,
        black: d.clock!.black,
        ticking: this.tickingClockColor(),
      });
    if (this.corresClock) this.corresClock.update(d.correspondence!.white, d.correspondence!.black);
    if (!this.replaying()) groundReload(this);
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.redraw();
    this.autoScroll();
    this.onChange();
    this.setLoading(false);
    this.pluginUpdate(d.steps[d.steps.length - 1].fen);
  };

  endWithData = (o: ApiEnd): void => {
    const d = this.data;
    d.game.winner = o.winner;
    d.game.status = o.status;
    d.game.boosted = o.boosted;
    d.player.blindfold = false;
    this.userJump(this.lastPly());
    d.game.fen = d.steps[d.steps.length - 1].fen;
    // If losing/drawing on time but locally it is the opponent's turn, move did not reach server before the end
    if (
      o.status.name === 'outoftime' &&
      d.player.color !== o.winner &&
      this.chessground.state.turnColor === d.opponent.color
    ) {
      this.reload(d);
    }
    this.promotion.cancel();
    this.chessground.stop();
    if (o.ratingDiff) {
      d.player.ratingDiff = o.ratingDiff[d.player.color];
      d.opponent.ratingDiff = o.ratingDiff[d.opponent.color];
    }
    if (!d.player.spectator && d.game.turns > 1) {
      poolRangeStorage.shiftRangeAfter(d);
      const key = o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw';
      // Delay 'victory' & 'defeat' sounds to avoid overlapping with 'checkmate' sound
      if (o.status.name === 'mate') site.sound.playAndDelayMateResultIfNecessary(key);
      else site.sound.play(key);
    }
    this.onTimeTrouble(false);
    endGameView();
    if (d.crazyhouse) crazyEndHook();
    this.clearJust();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock)
      this.clock.setClock({
        white: o.clock.wc * 0.01,
        black: o.clock.bc * 0.01,
        ticking: undefined,
      });
    this.redraw();
    this.autoScroll();
    this.onChange();
    if (d.tv) setTimeout(site.reload, 10000);
    wakeLock.release();
    if (this.data.game.status.name === 'started') site.sound.saySan(this.stepAt(this.ply).san, false);
    else site.sound.say(viewStatus(this.data), false, false, true);
    this.server.alive();
    if (
      !d.player.spectator &&
      o.status.name === 'outoftime' &&
      this.chessground.state.turnColor === d.opponent.color
    ) {
      notify(viewStatus(this.data));
    }
  };

  challengeRematch = async (): Promise<void> => {
    if (this.data.game.id !== 'synthetic') await xhr.challengeRematch(this.data.game.id);
    pubsub.emit('challenge-app.open');
    if (once('rematch-challenge')) {
      setTimeout(async () => {
        const [tour] = await Promise.all([
          site.asset.loadEsm<RoundTour>('round.tour'),
          site.asset.loadCssPath('bits.shepherd'),
        ]);
        tour.corresRematchOffline();
      }, 1000);
    }
  };

  private updateClockCtrl() {
    const d = this.data;
    if (d.clock) {
      this.corresClock = undefined;
      this.clock ??= new ClockCtrl(d.clock, d.pref, this.tickingClockColor(), this.makeClockOpts());
      this.clock.alarmAction = {
        seconds: 60,
        fire: () => this.onTimeTrouble(true),
      };
    } else {
      this.clock = undefined;
      if (d.correspondence)
        this.corresClock ??= new CorresClockController(this, d.correspondence, this.socket.outoftime);
    }
  }

  private makeClockOpts: () => ClockOpts = () => ({
    onFlag: this.socket.outoftime,
    bothPlayersHavePlayed: () => game.bothPlayersHavePlayed(this.data),
    hasGoneBerserk: this.hasGoneBerserk,
    alarmColor:
      this.data.simul || this.data.player.spectator || !this.data.pref.clockSound
        ? undefined
        : this.data.player.color,
  });

  private tickingClockColor = (): Color | undefined =>
    game.playable(this.data) && (game.playedTurns(this.data) > 1 || this.data.clock?.running)
      ? this.data.game.player
      : undefined;

  private setQuietMode = () => {
    const was = site.quietMode;
    const is = this.isPlaying();
    if (was !== is) {
      site.quietMode = is;
      $('body').toggleClass(
        'no-select',
        is && this.clock && this.clock.millisOf(this.data.player.color) <= 3e5,
      );
    }
  };

  question = (): QuestionOpts | false => {
    if (this.toSubmit) {
      setTimeout(() => this.voiceMove?.listenForResponse('submitMove', this.submitMove));
      return {
        prompt: i18n.site.confirmMove,
        yes: { action: () => this.submitMove(true) },
        no: { action: () => this.submitMove(false), text: i18n.site.cancel },
      };
    } else if (this.data.player.proposingTakeback) {
      this.voiceMove?.listenForResponse('cancelTakeback', this.cancelTakebackPreventDraws);
      return {
        prompt: i18n.site.takebackPropositionSent,
        no: { action: this.cancelTakebackPreventDraws, text: i18n.site.cancel },
      };
    } else if (this.data.player.offeringDraw) return { prompt: i18n.site.drawOfferSent };
    else if (this.data.opponent.offeringDraw)
      return {
        prompt: i18n.site.yourOpponentOffersADraw,
        yes: { action: () => this.socket.send('draw-yes'), icon: licon.OneHalf },
        no: { action: () => this.socket.send('draw-no') },
      };
    else if (this.data.opponent.proposingTakeback)
      return {
        prompt: i18n.site.yourOpponentProposesATakeback,
        yes: { action: this.takebackYes, icon: licon.Back },
        no: { action: () => this.socket.send('takeback-no') },
      };
    else if (this.voiceMove) return this.voiceMove.question();
    else return false;
  };

  opponentRequest(req: 'takeback' | 'rematch' | 'draw', text: string): void {
    this.voiceMove?.listenForResponse(req, (v: boolean) =>
      this.socket.sendLoading(`${req}-${v ? 'yes' : 'no'}`),
    );
    notify(text);
  }

  takebackYes = (): void => {
    this.socket.sendLoading('takeback-yes');
    this.chessground.cancelPremove();
    this.promotion.cancel();
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
  };

  hasGoneBerserk = (color: Color): boolean => !!this.goneBerserk[color];

  goBerserk = (): void => {
    if (game.berserkableBy(this.data) && !this.hasGoneBerserk(this.data.player.color)) {
      this.socket.berserk();
      site.sound.play('berserk');
    }
  };

  setBerserk = (color: Color): void => {
    if (this.goneBerserk[color]) return;
    this.goneBerserk[color] = true;
    if (color !== this.data.player.color) site.sound.play('berserk');
    this.redraw();
    $(`<i data-icon="${licon.Berserk}">`).appendTo($(`.game__meta .player.${color} .user-link`));
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
    site.unload.expected = true;
    setTimeout(() => {
      this.redirecting = false;
      this.redraw();
    }, 2500);
    this.redraw();
  };

  submitMove = (v: boolean): void => {
    if (!this.toSubmit) return;

    const submit = this.toSubmit;
    this.toSubmit = undefined;
    this.setLoading(true, 300);

    if (v) {
      this.actualSendMove('u' in submit ? 'move' : 'drop', submit);
      site.sound.play('confirmation');
    } else this.jump(this.ply);
  };

  private onChange = () => {
    if (this.opts.onChange) setTimeout(() => this.opts.onChange(this.data), 150);
  };

  private goneTick?: number;
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
      defined(d.opponent.isGone) &&
      d.opponent.isGone !== false &&
      !game.isPlayerTurn(d) &&
      game.resignable(d) &&
      d.opponent.isGone
    );
  };

  rematch(accept?: boolean): boolean {
    if (accept === undefined)
      return !!this.data.opponent.offeringRematch || !!this.data.player.offeringRematch;
    else if (accept) {
      if (this.data.game.rematch) location.href = gameRoute(this.data.game.rematch, this.data.opponent.color);
      if (!game.rematchable(this.data)) return false;
      if (!this.data.opponent.offeringRematch) this.data.player.offeringRematch = true;
      this.socket.send('rematch-yes');
    } else {
      if (!this.data.opponent.offeringRematch) return false;
      this.socket.send('rematch-no');
    }
    this.redraw();
    return true;
  }

  canOfferDraw = (): boolean =>
    !this.preventDrawOffer &&
    game.drawable(this.data) &&
    (this.data.player.lastDrawOfferAtPly || -99) < this.lastPly() - 20;

  cancelTakebackPreventDraws = (): void => {
    this.socket.sendLoading('takeback-no');
    clearTimeout(this.preventDrawOffer);
    this.preventDrawOffer = setTimeout(() => {
      this.preventDrawOffer = undefined;
      this.redraw();
    }, 4000);
  };

  offerDraw = (v: boolean, immediately?: boolean): void => {
    if (this.canOfferDraw()) {
      if (this.drawConfirm) {
        if (v) this.doOfferDraw();
        clearTimeout(this.drawConfirm);
        this.drawConfirm = undefined;
      } else if (v) {
        if (this.data.pref.confirmResign && !immediately)
          this.drawConfirm = setTimeout(() => {
            this.offerDraw(false);
          }, 3000);
        else this.doOfferDraw();
      }
    }
    this.redraw();
  };

  private doOfferDraw = () => {
    this.data.player.lastDrawOfferAtPly = this.lastPly();
    this.socket.sendLoading('draw-yes');
  };

  setChessground = (cg: CgApi): void => {
    this.chessground = cg;
    const up = { fen: this.stepAt(this.ply).fen, canMove: this.canMove(), cg };
    pubsub.on('board.change', (is3d: boolean) => {
      this.chessground.state.addPieceZIndex = is3d;
      this.chessground.redrawAll();
    });
    if (!this.isPlaying()) return;
    if (this.data.pref.keyboardMove) {
      if (!this.keyboardMove) this.keyboardMove = makeKeyboardMove(this);
      this.keyboardMove.update(up);
    }
    if (this.data.pref.voiceMove) {
      if (this.voiceMove) this.voiceMove.update(up);
      else this.voiceMove = makeVoiceMove(this, up);
    }
    if (this.keyboardMove || this.voiceMove) requestAnimationFrame(() => this.redraw());
  };

  stepAt = (ply: Ply): Step => util.plyStep(this.data, ply);

  speakClock = (): void => {
    this.clock?.speak();
  };

  blindfold = (v?: boolean): boolean => {
    this.data.player.blindfold ??= false;
    if (v === undefined || v === this.data.player.blindfold) return this.data.player.blindfold ?? false;
    this.blindfoldStorage.set(v);
    this.data.player.blindfold = v;
    this.socket.send(`blindfold-${v ? 'yes' : 'no'}`);
    this.redraw();
    return v;
  };

  onTimeTrouble = (t: boolean): void => {
    if (this.data.player.spectator) return;
    site.powertip.forcePlacementHook = t ? (el: HTMLElement) => el.closest('.crosstable') && 's' : undefined;
    this.chessground.state.touchIgnoreRadius = t ? Math.SQRT2 : 1;
  };

  yeet = (): void => {
    if (!this.data.player.spectator) this.doYeet();
  };

  private doYeet = memoize(() => {
    this.chessground.stop();
    site.asset.loadEsm('round.yeet');
  });

  private delayedInit = () =>
    requestIdleCallback(
      () => {
        const d = this.data;
        if (this.isPlaying()) {
          if (!d.simul) blur.init(d.steps.length > 2);

          title.init();
          this.setTitle();

          if (d.crazyhouse) crazyInit(this);

          if (!this.nvui && d.clock && !d.opponent.ai && !this.isSimulHost() && !d.local)
            window.addEventListener('beforeunload', e => {
              if (site.unload.expected || !this.isPlaying()) return;
              this.socket.send('bye2');
              e.preventDefault();
            });

          if (!this.nvui && d.pref.submitMove) {
            site.mousetrap
              .bind('esc', () => {
                this.submitMove(false);
                this.chessground.cancelMove();
              })
              .bind('return', () => this.submitMove(true));
          }
          cevalSub.subscribe(this);
        }

        if (!this.nvui) keyboardInit(this);
        if (this.isPlaying() && d.steps.length === 1) {
          this.blindfold(this.blindfoldStorage.get());
        }
        if (!d.local && d.game.speed !== 'correspondence') wakeLock.request();

        bindBookmarkButton(() => ({
          ply: this.ply,
        }));
      },

      800,
    );
}
