import * as co from 'chessops';
import { RoundProxy } from './roundProxy';
import { type GameContext, type GameStatus, LocalGame } from './localGame';
import { statusOf, clockToSpeed, playable } from 'lib/game';
import type { ClockData } from 'round';
import type { LocalPlayOpts, LocalSetup, SoundEvent, LocalSpeed } from 'lib/bot/types';
import { env } from './devEnv';
import { pubsub } from 'lib/pubsub';
import { myUserId, myUsername } from 'lib';

export interface GameObserver {
  hurry: boolean;
  beforeMove(uci: string): void;
  afterMove(moveCtx: any): void;
  onGameOver(status: any): boolean;
}

export class GameCtrl {
  live: LocalGame;
  rewind?: LocalGame;
  proxy: RoundProxy;
  clock?: ClockData & { since?: number };
  orientation: Color = 'white';
  observer?: GameObserver;

  private stopped = true;
  private resolveThink?: () => void;

  constructor(readonly opts: LocalPlayOpts) {
    pubsub.on('ply', this.jump);
    pubsub.on('flip', () => env.redraw());
    this.proxy = new RoundProxy(opts.pref);
  }

  load(game: LocalSetup | undefined): void {
    this.stop();
    this.rewind = undefined;
    this.live = new LocalGame({ ...this.live?.setup, ...game });
    env.bot.setUids(this.live);
    env.bot.reset();
    this.orientation = this.black ? 'white' : this.white ? 'black' : 'white';
    this.resetClock();
    this.proxy.reset();
    this.updateClockUi();
    env.round?.redraw();
    this.triggerStart(this.live.ply > 1 && Number.isFinite(this.initial));
  }

  start(): void {
    this.stopped = false;
    if (this.rewind) this.live = this.rewind;
    this.rewind = undefined;
    if (!this.live.finished) this.updateTurn(); // ??
  }

  stop(): void {
    if (this.isStopped) return;
    this.stopped = true;
    this.resolveThink?.();
  }

  flag(): void {
    if (this.clock) this.clock[this.live.turn] = 0;
    this.live.finish({ winner: this.live.awaiting, status: statusOf('outoftime') });
    this.gameOver({ winner: this.live.awaiting, status: statusOf('outoftime') });
    this.updateClockUi();
  }

  resign(): void {
    this.gameOver({ winner: this.live.awaiting, status: statusOf('resign') });
  }

  draw(): void {
    this.gameOver({ winner: undefined, status: statusOf('draw') });
  }

  nameOf(color: Color): string {
    if (!this[color] && this[co.opposite(color)]) return myUsername() ?? 'Anonymous';
    return this[color] ? (env.bot.info(this[color])?.name ?? this[color]) : i18n.site[color];
  }

  idOf(color: Color): string {
    return this[color] ?? myUserId() ?? 'anonymous';
  }

  get isStopped(): boolean {
    return this.stopped; // ??
  }

  get isLive(): boolean {
    return this.rewind === undefined && !this.isStopped;
  }

  get screenOrientation(): Color {
    return env.round?.flip ? co.opposite(this.orientation) : this.orientation;
  }

  get speed(): LocalSpeed {
    return clockToSpeed(this.initial, this.increment);
  }

  get white(): string | undefined {
    return this.live?.white;
  }

  get black(): string | undefined {
    return this.live?.black;
  }

  get initial(): number {
    return this.clock?.initial ?? Infinity;
  }

  get increment(): number {
    return this.clock?.increment ?? 0;
  }

  move(uci: Uci): void {
    if (this.rewind) this.live = this.rewind;
    this.rewind = undefined;
    this.stopped = false;
    this.observer?.beforeMove(uci);

    if (this.clock?.since) this.clock[this.live.turn] -= (performance.now() - this.clock.since) / 1000;
    const moveCtx = this.live.move({ uci, clock: this.clock });

    this.proxy.data.steps.splice(this.live.moves.length);

    this.observer?.afterMove?.(moveCtx);

    this.playSounds(moveCtx);
    env.round.apiMove(moveCtx);

    if (moveCtx.move?.promotion)
      env.round.chessground?.setPieces(
        new Map([
          [
            uci.slice(2, 4) as Key,
            { color: this.live.awaiting, role: moveCtx.move.promotion, promoted: true },
          ],
        ]),
      );

    if (this.live.finished) this.gameOver(moveCtx);
    else env.db.put(this.live);

    if (this.clock?.increment) this.clock[this.live.awaiting] += this.clock.increment;

    this.updateClockUi();

    if (!this.observer) window.location.hash = `id=${this.live.id}`;
    env.redraw();
  }

  private async maybeBotMove(): Promise<void> {
    const bot = env.bot[this.live.turn];
    const game = this.live;
    if (!bot || game.finished || this.isStopped || this.resolveThink) return;
    const move = await env.bot.move({
      pos: { fen: game.setupFen, moves: game.moves.map(x => x.uci) },
      ply: game.ply,
      chess: game.chess,
      avoid: game.threefoldMoves,
      initial: this.clock?.initial ?? Infinity,
      increment: this.clock?.increment ?? 0,
      remaining: this.clock?.[game.turn] ?? Infinity,
      opponentRemaining: this.clock?.[game.awaiting] ?? Infinity,
    });
    if (!move) return;
    await new Promise<void>(resolve => {
      if (this.clock) {
        this.clock[game.turn] -= move.movetime;
        this.clock.since = undefined;
      }
      if (this.observer?.hurry) return resolve();
      this.resolveThink = resolve;
      const realWait = Math.min(1 + Math.random(), game.ply > 0 ? move.movetime : 0);
      setTimeout(resolve, realWait * 1000);
    });
    this.resolveThink = undefined;
    if (!this.isStopped && game === this.live && env.round.ply === game.ply) this.move(move.uci);
    else setTimeout(() => this.updateTurn(), 200);
  }

  // investigate setting rewind = live to pause
  private jump = (ply: number) => {
    this.rewind = ply < this.live.moves.length ? new LocalGame(this.live, ply) : undefined;
    if (this.clock) this.clock.since = this.rewind || ply < 2 ? undefined : performance.now();
    this.updateTurn();
    setTimeout(env.redraw);
  };

  private updateTurn(game: LocalGame = this.rewind ?? this.live) {
    if (this.clock && game !== this.live) this.clock = { ...this.clock, ...game.clock };
    this.proxy.updateBoard(game, game.ply === 0 ? { lastMove: undefined } : {});
    this.updateClockUi();
    if (this.isLive) this.maybeBotMove();
  }

  private updateClockUi() {
    if (!this.clock) return;
    this.clock.running = this.isLive && this.live.ply > 1 && !this.isStopped;
    env.round?.clock?.setClock({
      white: this.clock.white,
      black: this.clock.black,
      ticking:
        playable(this.proxy.data) && this.proxy.data.clock!.running ? this.proxy.data.game.player : undefined,
    });
    if (this.isStopped || !this.isLive) env.round?.clock?.stopClock();
  }

  private gameOver(final: GameStatus) {
    this.stop();
    if (this.clock) env.round.clock?.stopClock();
    this.live.finish(final);
    env.db.put(this.live);

    env.round.endWithData?.({ status: final.status, winner: final.winner, boosted: false });
    this.observer?.onGameOver(final);
  }

  private playSounds(moveCtx: GameContext) {
    if (moveCtx.silent) return;
    const justPlayed = this.live.awaiting;
    const botInfo = env.bot[justPlayed] ?? env.bot[co.opposite(justPlayed)];
    if (!botInfo || !env.bot.bots.get(botInfo.uid)) return;
    const { san } = moveCtx;
    const sounds: SoundEvent[] = [];
    const prefix = env.bot[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.live.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (this.live.finished) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const bot = env.bot.bots.get(botInfo.uid)!;
    const boardSoundVolume = sounds.length ? bot.playSound(sounds) : 1;
    if (boardSoundVolume) site.sound.move({ ...moveCtx, volume: boardSoundVolume });
  }

  private triggerStart(inProgress = false) {
    for (const c of co.COLORS) {
      if (!env.bot[c]) continue;
      env.bot.bots.get(env.bot[c].uid)?.playSound(['greeting']);
    }
    if (!env.bot[this.live.turn] || env.bot[this.live.awaiting]) return;
    if (!inProgress) {
      setTimeout(() => this.start(), 200);
      return;
    }
    // TODO fix this temporary hack for the case when an in-progress realtime game is resumed
    // and it is the bot's turn. we don't want to start the clock until the user is ready,
    // so the current idea is to launch into a "paused" state that requires a click
    // on the board to trigger the bot to resume the game. i'm sure there's a better way.
    const main = document.querySelector<HTMLElement>('#main-wrap');
    main?.classList.add('paused');
    setTimeout(() => {
      const board = main?.querySelector<HTMLElement>('cg-container');
      const onclick = () => {
        main?.classList.remove('paused');
        this.start();
        board?.removeEventListener('click', onclick);
      };
      board?.addEventListener('click', onclick);
    }, 200);
  }

  private resetClock() {
    const initial = this.live.initial as number;
    this.clock = Number.isFinite(initial)
      ? {
          initial: initial,
          increment: this.live.increment ?? 0,
          white: this.live.clock?.white ?? initial,
          black: this.live.clock?.black ?? initial,
          running: false,
          since: undefined,
          moretime: 0,
        }
      : undefined;
  }
}
