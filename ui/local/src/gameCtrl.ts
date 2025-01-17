import * as co from 'chessops';
import { RoundProxy } from './roundProxy';
import { type MoveContext, type GameStatus, LocalGame } from 'game/localGame';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { type LocalSetup, clockToSpeed } from 'game';
import type { ClockData } from 'round';
import type { LocalPlayOpts, SoundEvent, LocalSpeed } from './types';
import { env } from './localEnv';
import { statusOf } from 'game/status';
import { pubsub } from 'common/pubsub';

export class GameCtrl {
  live: LocalGame;
  history?: LocalGame;
  proxy: RoundProxy;
  clock?: ClockData & { since?: number };
  orientation: Color;
  store: ObjectStorage<LocalGame>;

  private stopped = true;
  //private setup: LocalSetup;
  private resolveThink?: () => void;

  constructor(readonly opts: LocalPlayOpts) {
    // this.setup =
    //   opts.setup ??
    //   JSON.parse(localStorage.getItem('assets' in opts ? 'local.dev.setup' : 'local.setup') ?? '{}');
    // this.setup.initial ??= Infinity;
    // this.setup.initialFen ??= co.fen.INITIAL_FEN;
    //this.live = new LocalGame({ setup: opts.setup ?? {} });
  }

  async init(): Promise<void> {
    this.store = await objectStorage<LocalGame>({ store: 'local.games' });
    const id = localStorage.getItem(`local.${env.user}.gameId`);
    const game = id ? await this.store.get(id) : undefined;
    this.live = new LocalGame({ game, setup: this.opts.setup ?? {} });
    this.orientation = this.black ? 'white' : this.white ? 'black' : 'white';
    console.log(this.live);
    env.bot.setUids(this.live);
    env.assets.preload();
    pubsub.on('ply', this.jump);
    pubsub.on('flip', env.redraw);
    this.resetClock();
    this.proxy = new RoundProxy();
    this.triggerStart();
  }

  reset(params?: LocalSetup): void {
    this.stop();
    this.history = undefined;
    this.live = new LocalGame({ setup: { ...this.live.setup, ...params } });
    env.bot.reset();
    env.bot.setUids(this.live);
    env.assets.preload();
    this.updateTurn();
    this.resetClock();
    this.proxy.reset();
    this.updateClockUi();
    this.triggerStart();
  }

  start(): void {
    this.stopped = false;
    if (this.history) this.live = this.history;
    this.history = undefined;
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
    return this[color] ? env.bot.get(this[color])!.name : i18n.site[color];
  }

  idOf(color: Color): string {
    return this[color] ?? env.user;
  }

  get isStopped(): boolean {
    return this.stopped; // ??
  }

  get isLive(): boolean {
    return this.history === undefined && !this.isStopped;
  }

  get orientationForReal(): Color {
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

  get initialFen(): string {
    return this.live.initialFen; //this.setup.initialFen ?? co.fen.INITIAL_FEN;
  }

  move(uci: Uci): void {
    if (this.history) this.live = this.history;
    this.history = undefined;
    this.stopped = false;
    env.dev?.beforeMove(uci);

    if (this.clock?.since) this.clock[this.live.turn] -= (performance.now() - this.clock.since) / 1000;
    const moveCtx = this.live.move({ uci, clock: this.clock });
    const { move, justPlayed } = moveCtx;

    this.proxy.data.steps.splice(this.live.ply);

    env.dev?.afterMove?.(moveCtx);

    this.playSounds(moveCtx);
    env.round.apiMove(moveCtx);

    if (move?.promotion)
      env.round.chessground?.setPieces(
        new Map([[uci.slice(2, 4) as Key, { color: justPlayed, role: move.promotion, promoted: true }]]),
      );

    if (this.live.finished) this.gameOver(moveCtx);
    this.store.put(this.live.id, structuredClone(this.live));
    localStorage.setItem(`local.${env.user}.gameId`, this.live.id);
    if (this.clock?.increment) {
      this.clock[justPlayed] += this.clock.increment;
      this.updateClockUi();
    }
    env.redraw();
  }

  private jump = (ply: number): void => {
    this.history = ply < this.live.moves.length ? new LocalGame({ game: this.live, ply }) : undefined;
    if (this.clock) this.clock.since = this.history ? undefined : performance.now();
    this.updateTurn();
    setTimeout(env.redraw);
  };

  private async maybeMakeBotMove(): Promise<void> {
    const [bot, game] = [env.bot[this.live.turn], this.live];
    if (!bot || game.finished || this.isStopped || this.resolveThink) return;
    const move = await env.bot.move({
      pos: { fen: game.initialFen, moves: game.moves.map(x => x.uci) },
      chess: this.live.chess,
      avoid: this.live.threefoldDraws,
      remaining: this.clock?.[this.live.turn],
      initial: this.clock?.initial,
      increment: this.clock?.increment,
    });
    if (!move) return;
    await new Promise<void>(resolve => {
      if (this.clock) {
        this.clock[this.live.turn] -= move.thinkTime;
        this.clock.since = undefined;
      }
      if (env.dev?.hurry) return resolve();
      this.resolveThink = resolve;
      const realWait = Math.min(1 + 2 * Math.random(), this.live.ply > 0 ? move.thinkTime : 0);
      setTimeout(resolve, realWait * 1000);
    });
    this.resolveThink = undefined;
    if (!this.isStopped && game === this.live && env.round.ply === game.ply) this.move(move.uci);
    else setTimeout(() => this.updateTurn(), 200);
  }

  private updateTurn(game: LocalGame = this.history ?? this.live): void {
    if (this.clock && game !== this.live) this.clock = { ...this.clock, ...game.clock };
    this.proxy.cg(game, game.ply === 0 ? { lastMove: undefined } : {});
    this.updateClockUi();
    if (this.isLive) this.maybeMakeBotMove();
  }

  private updateClockUi(): void {
    if (!this.clock) return;
    this.clock.running = this.isLive && this.live.ply > 0;
    env.round.clock?.setClock(this.proxy.data, this.clock.white, this.clock.black);
    if (this.isStopped || !this.isLive) env.round.clock?.stopClock();
  }

  private gameOver(final: Omit<GameStatus, 'end' | 'turn'>) {
    this.stop();
    if (this.clock) env.round.clock?.stopClock();
    if (!env.dev?.onGameOver({ ...final, turn: this.live.turn })) {
      env.round.endWithData?.({ ...final, boosted: false });
    }
    env.redraw();
  }

  private playSounds(moveCtx: MoveContext): void {
    if (moveCtx.silent) return;
    const { justPlayed, san } = moveCtx;
    const sounds: SoundEvent[] = [];
    const prefix = env.bot[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.live.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (this.live.finished) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const boardSoundVolume = sounds ? env.bot.playSound(justPlayed, sounds) : 1;
    if (boardSoundVolume) site.sound.move({ ...moveCtx, volume: boardSoundVolume });
  }

  private triggerStart(): void {
    ['white', 'black'].forEach(c => env.bot.playSound(c as Color, ['greeting']));
    setTimeout(() => !env.dev && env.bot[this.live.turn] && this.start(), 500);
  }

  private resetClock(): void {
    const initial = this.live.initial as number;
    this.clock = Number.isFinite(initial)
      ? {
          initial: initial,
          increment: this.live.increment ?? 0,
          white: initial,
          black: initial,
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
