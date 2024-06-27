/// <reference types="../types/ab" />

import * as ab from 'ab';
import * as round from './round';
import * as game from 'game';
import { game as gameRoute } from 'game/router';
import * as status from 'game/status';
import * as ground from './ground';
import * as licon from 'common/licon';
import notify from 'common/notification';
import { make as makeSocket, RoundSocket } from './socket';
import * as title from './title';
import * as blur from './blur';
import viewStatus from 'game/view/status';
import * as cg from 'chessground/types';
import { Config as CgConfig } from 'chessground/config';
import { Api as CgApi } from 'chessground/api';
import { ClockController } from './clock/clockCtrl';
import { CorresClockController } from './corresClock/corresClockCtrl';
import MoveOn from './moveOn';
import TransientMove from './transientMove';
import * as atomic from './atomic';
import * as util from './util';
import * as xhr from './xhr';
import { valid as crazyValid, init as crazyInit, onEnd as crazyEndHook } from './crazy/crazyCtrl';
import { MoveRootCtrl } from 'chess/moveRootCtrl';
import { ctrl as makeKeyboardMove, KeyboardMove } from 'keyboardMove';
import { makeVoiceMove, VoiceMove } from 'voice';
import * as renderUser from './view/user';
import * as cevalSub from './cevalSub';
import * as keyboard from './keyboard';
import { PromotionCtrl, promote } from 'chess/promotion';
import * as wakeLock from 'common/wakeLock';
import { opposite, uciToMove } from 'chessground/util';
import * as Prefs from 'common/prefs';
import { endGameView } from './view/main';
import {
  RoundOpts,
  RoundData,
  ApiMove,
  ApiEnd,
  SocketMove,
  SocketDrop,
  SocketOpts,
  MoveMetadata,
  Position,
  NvuiPlugin,
  RoundTour,
} from './interfaces';
import { defined, Toggle, toggle, requestIdleCallback } from 'common';
import { Redraw } from 'common/snabbdom';

interface GoneBerserk {
  white?: boolean;
  black?: boolean;
}

type Timeout = number;

export default class RoundController implements MoveRootCtrl {
  data: RoundData;
  socket: RoundSocket;
  chessground: CgApi;
  clock?: ClockController;
  corresClock?: CorresClockController;
  trans: Trans;
  noarg: TransNoArg;
  keyboardMove?: KeyboardMove;
  voiceMove?: VoiceMove;
  moveOn: MoveOn;
  promotion: PromotionCtrl;
  ply: number;
  firstSeconds = true;
  flip = false;
  menu: Toggle;
  confirmMoveEnabled: Toggle = toggle(true);
  loading = false;
  loadingTimeout: number;
  redirecting = false;
  transientMove: TransientMove;
  moveToSubmit?: SocketMove;
  dropToSubmit?: SocketDrop;
  goneBerserk: GoneBerserk = {};
  resignConfirm?: Timeout = undefined;
  drawConfirm?: Timeout = undefined;
  preventDrawOffer?: Timeout = undefined;
  // will be replaced by view layer
  autoScroll: () => void = () => {};
  justDropped?: cg.Role;
  justCaptured?: cg.Piece;
  shouldSendMoveTime = false;
  preDrop?: cg.Role;
  sign: string = Math.random().toString(36);
  keyboardHelp: boolean = location.hash === '#keyboard';
  blindfoldStorage: LichessBooleanStorage;

  constructor(
    readonly opts: RoundOpts,
    readonly redraw: Redraw,
    readonly nvui?: NvuiPlugin,
  ) {
    round.massage(opts.data);

    const d = (this.data = opts.data);

    this.ply = round.lastPly(d);
    this.goneBerserk[d.player.color] = d.player.berserk;
    this.goneBerserk[d.opponent.color] = d.opponent.berserk;
    setTimeout(() => {
      this.firstSeconds = false;
      this.redraw();
    }, 3000);

    this.socket = makeSocket(opts.socketSend, this);
    this.blindfoldStorage = site.storage.boolean(`blindfold.${this.data.player.user?.id ?? 'anon'}`);

    if (d.clock)
      this.clock = new ClockController(d, {
        onFlag: this.socket.outoftime,
        soundColor: d.simul || d.player.spectator || !d.pref.clockSound ? undefined : d.player.color,
        nvui: !!this.nvui,
      });
    else {
      this.makeCorrespondenceClock();
      setInterval(this.corresClockTick, 1000);
    }

    this.promotion = new PromotionCtrl(
      f => f(this.chessground),
      () => {
        this.chessground.cancelPremove();
        xhr.reload(this).then(this.reload, site.reload);
      },
      this.redraw,
      d.pref.autoQueen,
    );

    this.setQuietMode();

    this.moveOn = new MoveOn(this, 'move-on');
    this.transientMove = new TransientMove(this.socket);

    this.menu = toggle(false, redraw);

    this.trans = site.trans(opts.i18n);
    this.noarg = this.trans.noarg;

    setTimeout(this.delayedInit, 200);

    setTimeout(this.showExpiration, 350);

    if (!document.referrer?.includes('/serviceWorker.')) setTimeout(this.showYourMoveNotification, 500);

    // at the end:
    site.pubsub.on('jump', ply => {
      this.jump(parseInt(ply));
      this.redraw();
    });

    site.pubsub.on('zen', () => {
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

  private onUserMove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) => {
    if (!this.keyboardMove?.usedSan) ab.move(this, meta);
    if (!this.startPromotion(orig, dest, meta)) this.sendMove(orig, dest, undefined, meta);
  };

  private onUserNewPiece = (role: cg.Role, key: cg.Key, meta: cg.MoveMetadata) => {
    if (!this.replaying() && crazyValid(this.data, role, key)) {
      this.sendNewPiece(role, key, !!meta.predrop);
    } else this.jump(this.ply);
  };

  private onMove = (orig: cg.Key, dest: cg.Key, captured?: cg.Piece) => {
    if (captured || this.enpassant(orig, dest)) {
      if (this.data.game.variant.key === 'atomic') {
        site.sound.play('explosion');
        atomic.capture(this, dest);
      } else site.sound.move({ name: 'capture', filter: 'game' });
    } else site.sound.move({ name: 'move', filter: 'game' });
  };

  private startPromotion = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) =>
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

  private onPremove = (orig: cg.Key, dest: cg.Key, meta: cg.MoveMetadata) =>
    this.startPromotion(orig, dest, meta);

  private onCancelPremove = () => this.promotion.cancelPrePromotion();

  private onNewPiece = (piece: cg.Piece, key: cg.Key): void => {
    if (piece.role === 'pawn' && (key[1] === '1' || key[1] === '8')) return;
    site.sound.move();
  };

  private onPredrop = (role: cg.Role | undefined, _?: Key) => {
    this.preDrop = role;
    this.redraw();
  };

  private isSimulHost = () => this.data.simul && this.data.simul.hostId === this.opts.userId;

  private enpassant = (orig: cg.Key, dest: cg.Key): boolean => {
    if (orig[0] === dest[0] || this.chessground.state.pieces.get(dest)?.role !== 'pawn') return false;
    const pos = (dest[0] + orig[1]) as cg.Key;
    this.chessground.setPieces(new Map([[pos, undefined]]));
    return true;
  };

  lastPly = () => round.lastPly(this.data);

  makeCgHooks = () => ({
    onUserMove: this.onUserMove,
    onUserNewPiece: this.onUserNewPiece,
    onMove: this.onMove,
    onNewPiece: this.onNewPiece,
    onPremove: this.onPremove,
    onCancelPremove: this.onCancelPremove,
    onPredrop: this.onPredrop,
  });

  replaying = (): boolean => this.ply !== this.lastPly();

  userJump = (ply: Ply): void => {
    this.cancelMove();
    this.chessground.selectSquare(null);
    if (ply != this.ply && this.jump(ply)) site.sound.saySan(this.stepAt(this.ply).san, true);
    else this.redraw();
  };

  userJumpPlyDelta = (plyDelta: Ply) => this.userJump(this.ply + plyDelta);

  isPlaying = () => game.isPlayerPlaying(this.data);

  jump = (ply: Ply): boolean => {
    ply = Math.max(round.firstPly(this.data), Math.min(this.lastPly(), ply));
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
    this.chessground.set(config);
    if (s.san && isForwardStep) site.sound.move(s);
    this.autoScroll();
    this.pluginUpdate(s.fen);
    site.pubsub.emit('ply', ply);
    return true;
  };

  canMove = () => !this.replaying() && this.data.player.color === this.chessground.state.turnColor;

  replayEnabledByPref = (): boolean => {
    const d = this.data;
    return (
      d.pref.replay === Prefs.Replay.Always ||
      (d.pref.replay === Prefs.Replay.OnlySlowGames &&
        (d.game.speed === 'classical' || d.game.speed === 'correspondence'))
    );
  };

  isLate = () => this.replaying() && status.playing(this.data);

  playerAt = (position: Position): game.Player =>
    (this.flip as any) ^ ((position === 'top') as any) ? this.data.opponent : this.data.player;

  flipNow = () => {
    this.flip = !this.nvui && !this.flip;
    this.chessground.set({
      orientation: ground.boardOrientation(this.data, this.flip),
    });
    this.redraw();
  };

  setTitle = () => title.set(this);

  actualSendMove = (tpe: string, data: any, meta: MoveMetadata = {}) => {
    const socketOpts: SocketOpts = {
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
    this.transientMove.register();
    this.redraw();
  };

  pluginMove = (orig: cg.Key, dest: cg.Key, role?: cg.Role) => {
    if (!role) {
      this.chessground.move(orig, dest);
      // TODO look into possibility of making cg.Api.move function update player turn itself.
      this.chessground.state.movable.dests = undefined;
      this.chessground.state.turnColor = opposite(this.chessground.state.turnColor);

      if (this.startPromotion(orig, dest, { premove: false })) return;
    }
    this.sendMove(orig, dest, role, { premove: false });
  };

  pluginUpdate = (fen: string) => {
    this.voiceMove?.update({ fen, canMove: this.canMove() });
    this.keyboardMove?.update({ fen, canMove: this.canMove() });
  };

  sendMove = (orig: cg.Key, dest: cg.Key, prom: cg.Role | undefined, meta: cg.MoveMetadata) => {
    const move: SocketMove = {
      u: orig + dest,
    };
    if (prom) move.u += prom === 'knight' ? 'n' : prom[0];
    if (blur.get()) move.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && this.confirmMoveEnabled() && !meta.premove) {
      this.moveToSubmit = move;
      this.redraw();
    } else {
      this.actualSendMove('move', move, {
        justCaptured: meta.captured,
        premove: meta.premove,
      });
    }
  };

  sendNewPiece = (role: cg.Role, key: cg.Key, isPredrop: boolean): void => {
    const drop: SocketDrop = { role, pos: key };
    if (blur.get()) drop.b = 1;
    this.resign(false);
    if (this.data.pref.submitMove && this.confirmMoveEnabled() && !isPredrop) {
      this.dropToSubmit = drop;
      this.redraw();
    } else {
      this.actualSendMove('drop', drop, {
        justDropped: role,
        premove: isPredrop,
      });
    }
  };

  showYourMoveNotification = () => {
    const d = this.data;
    const opponent = $('body').hasClass('zen') ? 'Your opponent' : renderUser.userTxt(this, d.opponent);
    const joined = `${opponent}\njoined the game.`;
    if (game.isPlayerTurn(d))
      notify(() => {
        let txt = this.noarg('yourTurn');
        if (this.ply < 1) txt = `${joined}\n${txt}`;
        else {
          let move = d.steps[d.steps.length - 1].san;
          const turn = Math.floor((this.ply - 1) / 2) + 1;
          move = `${turn}${this.ply % 2 === 1 ? '.' : '...'} ${move}`;
          txt = `${opponent}\nplayed ${move}.\n${txt}`;
        }
        return txt;
      });
    else if (this.isPlaying() && this.ply < 1) notify(joined);
  };

  playerByColor = (c: Color) => this.data[c === this.data.player.color ? 'player' : 'opponent'];

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
          o.uci.slice(2, 4) as cg.Key,
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
      if (o.check) site.sound.play('check');
      blur.onMove();
      site.pubsub.emit('ply', this.ply);
    }
    d.game.threefold = !!o.threefold;
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
      if (this.clock) this.clock.setClock(d, oc.white, oc.black, delay);
      else if (this.corresClock) this.corresClock.update(oc.white, oc.black);
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
      // https://github.com/lichess-org/lila/issues/343
      const premoveDelay = d.game.variant.key === 'atomic' ? 100 : 1;
      setTimeout(() => {
        if (this.nvui) this.nvui.playPremove(this);
        else if (!this.chessground.playPremove() && !this.playPredrop()) {
          this.promotion.cancel();
          this.showYourMoveNotification();
        }
      }, premoveDelay);
    }
    this.autoScroll();
    this.onChange();
    this.pluginUpdate(step.fen);
    site.sound.move({ ...o, filter: 'music' });
    site.sound.saySan(step.san);
    return true; // prevents default socket pubsub
  };

  crazyValid = (role: cg.Role, key: cg.Key) => crazyValid(this.data, role, key);

  getCrazyhousePockets = () => this.data.crazyhouse?.pockets;

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
      const key = o.winner ? (d.player.color === o.winner ? 'victory' : 'defeat') : 'draw';
      site.sound.play(key);
      if (
        key != 'victory' &&
        d.game.turns > 6 &&
        !d.tournament &&
        !d.swiss &&
        site.storage.boolean('courtesy').get()
      )
        this.opts.chat?.instance?.then(c => c.post('Good game, well played'));
    }
    endGameView();
    if (d.crazyhouse) crazyEndHook();
    this.clearJust();
    this.setTitle();
    this.moveOn.next();
    this.setQuietMode();
    this.setLoading(false);
    if (this.clock && o.clock) this.clock.setClock(d, o.clock.wc * 0.01, o.clock.bc * 0.01);
    this.redraw();
    this.autoScroll();
    this.onChange();
    if (d.tv) setTimeout(site.reload, 10000);
    wakeLock.release();
    if (this.data.game.status.name === 'started') site.sound.saySan(this.stepAt(this.ply).san, false);
    else site.sound.say(viewStatus(this), false, false, true);
  };

  challengeRematch = async () => {
    await xhr.challengeRematch(this.data.game.id);
    site.pubsub.emit('challenge-app.open');
    if (site.once('rematch-challenge')) {
      setTimeout(async () => {
        const [tour] = await Promise.all([
          site.asset.loadEsm<RoundTour>('round.tour'),
          site.asset.loadCssPath('bits.shepherd'),
        ]);
        tour.corresRematchOffline();
      }, 1000);
    }
  };

  private makeCorrespondenceClock = (): void => {
    if (this.data.correspondence && !this.corresClock)
      this.corresClock = new CorresClockController(this, this.data.correspondence, this.socket.outoftime);
  };

  private corresClockTick = (): void => {
    if (this.corresClock && game.playable(this.data)) {
      this.corresClock.tick(this.data.game.player);
      this.redraw();
    }
  };

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
    if (this.moveToSubmit || this.dropToSubmit) {
      setTimeout(() => this.voiceMove?.listenForResponse('submitMove', this.submitMove));
      return {
        prompt: this.noarg('confirmMove'),
        yes: { action: () => this.submitMove(true) },
        no: { action: () => this.submitMove(false), key: 'cancel' },
      };
    } else if (this.data.player.proposingTakeback) {
      this.voiceMove?.listenForResponse('cancelTakeback', this.cancelTakebackPreventDraws);
      return {
        prompt: this.noarg('takebackPropositionSent'),
        no: { action: this.cancelTakebackPreventDraws, key: 'cancel' },
      };
    } else if (this.data.player.offeringDraw) return { prompt: this.noarg('drawOfferSent') };
    else if (this.data.opponent.offeringDraw)
      return {
        prompt: this.noarg('yourOpponentOffersADraw'),
        yes: { action: () => this.socket.send('draw-yes'), icon: licon.OneHalf },
        no: { action: () => this.socket.send('draw-no') },
      };
    else if (this.data.opponent.proposingTakeback)
      return {
        prompt: this.noarg('yourOpponentProposesATakeback'),
        yes: { action: this.takebackYes, icon: licon.Back },
        no: { action: () => this.socket.send('takeback-no') },
      };
    else if (this.voiceMove) return this.voiceMove.question();
    else return false;
  };

  opponentRequest(req: string, i18nKey: string) {
    this.voiceMove?.listenForResponse(req, (v: boolean) =>
      this.socket.sendLoading(`${req}-${v ? 'yes' : 'no'}`),
    );
    notify(this.noarg(i18nKey));
  }

  takebackYes = () => {
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

  goBerserk = () => {
    if (!game.berserkableBy(this.data)) return;
    if (this.goneBerserk[this.data.player.color]) return;
    this.socket.berserk();
    site.sound.play('berserk');
  };

  setBerserk = (color: Color): void => {
    if (this.goneBerserk[color]) return;
    this.goneBerserk[color] = true;
    if (color !== this.data.player.color) site.sound.play('berserk');
    this.redraw();
    $(`<i data-icon="${licon.Berserk}">`).appendTo($(`.game__meta .player.${color} .user-link`));
  };

  setLoading = (v: boolean, duration = 1500) => {
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
    site.unload.expected = true;
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
      site.sound.play('confirmation');
    } else this.jump(this.ply);
    this.cancelMove();
    //cancel premove when you cancel move
    if (!v && toSubmit) {
      this.chessground.cancelPremove();
    }
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
      return this.data.opponent.offeringRematch === true || this.data.player.offeringRematch === true;
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
    (this.data.player.lastDrawOfferAtPly || -99) < this.ply - 20;

  cancelTakebackPreventDraws = () => {
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
    this.data.player.lastDrawOfferAtPly = this.ply;
    this.socket.sendLoading('draw-yes', null);
  };

  setChessground = (cg: CgApi) => {
    this.chessground = cg;
    const up = { fen: this.stepAt(this.ply).fen, canMove: this.canMove(), cg };
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
    site.pubsub.on('board.change', (is3d: boolean) => {
      this.chessground.state.addPieceZIndex = is3d;
      this.chessground.redrawAll();
    });
  };

  stepAt = (ply: Ply) => round.plyStep(this.data, ply);

  speakClock = () => {
    this.clock?.speak();
  };

  blindfold = (v?: boolean): boolean => {
    if (v === undefined || v === this.data.player.blindfold) return this.data.player.blindfold ?? false;
    this.blindfoldStorage.set(v);
    this.data.player.blindfold = v;
    this.socket.send(`blindfold-${v ? 'yes' : 'no'}`);
    this.redraw();
    return v;
  };

  private delayedInit = () => {
    requestIdleCallback(() => {
      const d = this.data;
      if (this.isPlaying()) {
        if (!d.simul) blur.init(d.steps.length > 2);

        title.init();
        this.setTitle();

        if (d.crazyhouse) crazyInit(this);

        if (!this.nvui && d.clock && !d.opponent.ai && !this.isSimulHost())
          window.addEventListener('beforeunload', e => {
            if (site.unload.expected || !this.isPlaying()) return;
            this.socket.send('bye2');
            const msg = 'There is a game in progress!';
            (e || window.event).returnValue = msg;
            return msg;
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

      if (!this.nvui) keyboard.init(this);
      if (this.isPlaying() && d.steps.length === 1) {
        if (site.storage.get('blindfold') === 'true') {
          // TODO - delete this if block & storage.set once a few weeks pass
          site.storage.remove('blindfold');
          this.blindfoldStorage.set(true);
        }

        this.blindfold(this.blindfoldStorage.get());
      }
      wakeLock.request();

      setTimeout(() => {
        if ($('#KeyboardO,#show_btn,#shadowHostId').length) {
          alert('Play enhancement extensions are no longer allowed!');
          site.socket.destroy();
          this.setRedirecting();
          location.href = '/page/play-extensions';
        }
      }, 1000);

      this.onChange();
    }, 800);
  };
}
