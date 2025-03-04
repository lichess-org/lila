import * as co from 'chessops';
import { RoundProxy } from './roundProxy';
import { type GameContext, type GameStatus, LocalGame } from './localGame';
import { statusOf, clockToSpeed } from 'game';
import type { ClockData } from 'round';
import type { LocalPlayOpts, LocalSetup, SoundEvent, LocalSpeed } from './types';
import { env } from './localEnv';
import { pubsub } from 'common/pubsub';

export class GameCtrl {
  live: LocalGame;
  rewind?: LocalGame;
  proxy: RoundProxy;
  clock?: ClockData & { since?: number };
  orientation: Color = 'white';

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
    if (!this[color] && this[co.opposite(color)]) return env.username;
    return this[color] ? (env.bot.get(this[color])?.name ?? this[color]) : i18n.site[color];
  }

  idOf(color: Color): string {
    return this[color] ?? env.user;
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
    env.dev?.beforeMove(uci);

    if (this.clock?.since) this.clock[this.live.turn] -= (performance.now() - this.clock.since) / 1000;
    const moveCtx = this.live.move({ uci, clock: this.clock });

    this.proxy.data.steps.splice(this.live.moves.length);

    env.dev?.afterMove?.(moveCtx);

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

    window.location.hash = `id=${this.live.id}`;
    env.redraw();
  }

  private async maybeBotMove(): Promise<void> {
    const bot = env.bot[this.live.turn];
    const game = this.live;
    if (!bot || game.finished || this.isStopped || this.resolveThink) return;
    const move = await env.bot.move({
      pos: { fen: game.setupFen, moves: game.moves.map(x => x.uci) },
      chess: game.chess,
      avoid: game.threefoldMoves,
      initial: this.clock?.initial ?? Infinity,
      remaining: this.clock?.[game.turn] ?? Infinity,
      opponentRemaining: this.clock?.[game.awaiting] ?? Infinity,
      increment: this.clock?.increment ?? 0,
      ply: game.ply,
    });
    if (!move) return;
    await new Promise<void>(resolve => {
      if (this.clock) {
        this.clock[game.turn] -= move.movetime;
        this.clock.since = undefined;
      }
      if (env.dev?.hurry) return resolve();
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
    env.round?.clock?.setClock(this.proxy.data, this.clock.white, this.clock.black);
    if (this.isStopped || !this.isLive) env.round?.clock?.stopClock();
  }

  private gameOver(final: GameStatus) {
    this.stop();
    if (this.clock) env.round.clock?.stopClock();
    this.live.finish(final);
    env.db.put(this.live);
    if (env.dev?.onGameOver(final)) return;
    // TODO - onGameOver conditional logic leak into gameCtrl, fix game scripting in devCtrl
    env.round.endWithData?.({ status: final.status, winner: final.winner, boosted: false });
  }

  private playSounds(moveCtx: GameContext) {
    if (moveCtx.silent) return;
    const justPlayed = this.live.awaiting;
    const { san } = moveCtx;
    const sounds: SoundEvent[] = [];
    const prefix = env.bot[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.live.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (this.live.finished) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const boardSoundVolume = sounds ? env.bot.playSound(justPlayed, sounds) : 1;
    if (boardSoundVolume) site.sound.move({ ...moveCtx, volume: boardSoundVolume });
  }

  private triggerStart(inProgress = false) {
    ['white', 'black'].forEach(c => env.bot.playSound(c as Color, ['greeting']));
    if (env.dev || !env.bot[this.live.turn]) return;
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
          emerg: 0,
          showTenths: this.opts.pref.clockTenths,
          showBar: true,
          moretime: 0,
          running: false,
          since: undefined,
        }
      : undefined;
  }
}
