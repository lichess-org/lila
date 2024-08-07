import * as co from 'chessops';
import { RoundProxy } from './roundProxy';
import { makeFen } from 'chessops/fen';
import { type MoveContext, type GameStatus, LocalGame } from './localGame';
import type { RoundOpts, RoundData, RoundController, ClockData } from 'round';
import type { Source } from 'game';
import type { LocalPlayOpts, LocalSetup, Automator, SoundEvent } from './types';
import type { BotCtrl } from './botCtrl';
import { statusOf } from 'game/status';

export class GameCtrl {
  private stopped = true;
  live: LocalGame;
  history?: LocalGame;
  proxy: RoundProxy;
  round: RoundController;
  clock?: ClockData;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  originalOrientation: Color;
  resolveThink?: () => void;
  dev?: Automator;
  countingSince: number | undefined;

  constructor(
    readonly opts: LocalPlayOpts,
    readonly botCtrl: BotCtrl,
    readonly redraw: () => void,
    readonly source: Source = 'local',
  ) {
    this.setup = opts.setup ?? {};
    this.originalOrientation = this.setup.black ? 'white' : this.setup.white ? 'black' : 'white';
    this.botCtrl.setUids(this.setup);
    this.i18n = opts.i18n;
    this.setup.fen ??= co.fen.INITIAL_FEN;
    this.resetClock();
    this.live = new LocalGame(this.setup.fen);
    this.proxy = new RoundProxy(this);
    this.onNewGame();
    site.pubsub.on('ply', this.jump);
    site.pubsub.on('flip', this.redraw);
  }

  get data(): RoundData {
    return this.proxy.data;
  }

  get chess(): co.Chess {
    return this.live.chess;
  }

  get turn(): Color {
    return this.chess.turn;
  }

  get viewing(): LocalGame {
    return this.history ?? this.live;
  }

  get pondering(): Color {
    return co.opposite(this.turn);
  }

  get status(): GameStatus {
    return this.live.status;
  }

  get isUserTurn(): boolean {
    return this.history === undefined && !this.botCtrl[this.turn];
  }

  get isStopped(): boolean {
    return this.stopped;
  }

  get fen(): string {
    return makeFen(this.chess.toSetup());
  }

  get isLive(): boolean {
    return this.history === undefined && !this.isStopped;
  }

  get ply(): number {
    return 2 * (this.chess.fullmoves - 1) + (this.turn === 'black' ? 1 : 0);
  }

  get cgOrientation(): Color {
    return this.round?.flip ? co.opposite(this.originalOrientation) : this.originalOrientation;
  }

  start(): void {
    this.stopped = false;
    setTimeout(() => !this.live.end && this.updateTurn());
  }

  onNewGame(): void {
    ['white', 'black'].forEach(c => this.botCtrl.playSound(c as Color, ['greeting']));
    setTimeout(() => !this.dev && !this.isUserTurn && this.start(), 500);
  }

  async stop(): Promise<void> {
    if (this.isStopped) return;
    this.stopped = true;
    this.resolveThink?.();
  }

  async botMove(): Promise<void> {
    const [bot, game] = [this.botCtrl[this.turn], this.live];
    if (!bot || game.end || this.isStopped || this.resolveThink) return;
    const move = await this.botCtrl.move({
      pos: { fen: game.initialFen, moves: game.moves.map(x => x.uci) },
      chess: this.chess,
      secondsRemaining: this.clock?.[this.turn],
      initial: this.clock?.initial,
      increment: this.clock?.increment,
    });
    if (!move) return;
    await new Promise<void>(resolve => {
      if (this.dev?.skip(move.time)) return resolve();
      this.resolveThink = resolve;
      setTimeout(resolve, move.time * 1000);
    });
    this.resolveThink = undefined;
    if (!this.isStopped && game === this.live && this.round.ply === game.ply) this.move(move.uci);
    else setTimeout(() => this.updateTurn(), 200);
  }

  reset(params: LocalSetup = this.setup): void {
    this.setup = { ...this.setup, ...params };
    this.botCtrl.setUids(this.setup);
    this.history = undefined;
    this.live = new LocalGame(this.setup.fen ?? co.fen.INITIAL_FEN);
    this.countingSince = undefined;
    this.stop();
    this.botCtrl.reset();
    this.updateTurn();
    this.resetClock();
    this.updateClock();
    this.proxy.reset();
    this.onNewGame();
  }

  resetClock(): void {
    const initial = this.setup.initial ?? 0;
    const increment = this.setup.increment ?? 0;
    if (initial === 0) {
      this.data.game.speed = this.data.game.perf = 'classical';
      this.clock = undefined;
      return;
    }
    this.clock = Object.defineProperty(
      {
        running: false,
        initial,
        increment,
        white: initial,
        black: initial,
        emerg: 0,
        showTenths: this.opts.pref.clockTenths,
        showBar: true,
        moretime: 0,
      },
      'running',
      { get: () => this.isLive && this.live.ply > 0 },
    );
  }

  flag(): void {
    if (this.clock) this.clock[this.turn] = 0;
    this.gameOver({ winner: this.pondering, status: statusOf('outoftime') });
    this.updateClock();
  }

  resign(): void {
    this.gameOver({ winner: this.pondering, status: statusOf('resign') });
  }

  draw(): void {
    this.gameOver({ winner: undefined, status: statusOf('draw') });
  }

  jump = (ply: number): void => {
    this.history =
      ply < this.live.moves.length ? new LocalGame(this.setup.fen, this.live.moves.slice(0, ply)) : undefined;
    this.countingSince = this.history ? undefined : performance.now();
    this.updateTurn();
  };

  move(uci: Uci): boolean {
    if (this.history) this.live = this.history;
    this.history = undefined;
    this.stopped = false;

    if (this.clock && this.countingSince)
      this.clock[this.turn] -= (performance.now() - this.countingSince) / 1000;
    const moveCtx = this.live.move({ uci, clock: this.clock });
    const { end, move, justPlayed } = moveCtx;

    this.data.steps.splice(this.live.ply);
    this.dev?.preMove?.(moveCtx);
    this.playSounds(moveCtx);
    this.round.apiMove(moveCtx);

    if (move?.promotion)
      this.round.chessground?.setPieces(
        new Map([[uci.slice(2, 4) as Cg.Key, { color: justPlayed, role: move.promotion, promoted: true }]]),
      );

    if (end) this.gameOver(moveCtx);
    if (this.clock?.increment && this.clock?.initial) {
      this.clock[justPlayed] += this.clock.increment;
      this.updateClock();
    }
    this.redraw();
    return !end;
  }

  updateTurn(game: LocalGame = this.history ?? this.live): void {
    this.proxy.updateCg(game, this.live.ply === 0 ? { lastMove: undefined } : {});
    if (this.clock && game !== this.live) this.clock = { ...this.clock, ...game.clock };
    this.updateClock();
    if (this.isLive) this.botMove();
  }

  private updateClock(): void {
    if (!this.clock) return;
    this.round.clock?.setClock(this.data, this.clock.white, this.clock.black);
    if (this.isStopped || !this.isLive) this.round.clock?.stopClock();
  }

  private gameOver(final: Omit<GameStatus, 'end' | 'turn'>) {
    this.live.finish(final);
    this.stop();
    if (this.clock) this.round.clock?.stopClock();
    if (!this.dev?.onGameOver({ ...final, end: true, turn: this.turn })) {
      this.round.endWithData?.({ ...final, boosted: false });
    }
    this.redraw();
  }

  private playSounds(moveCtx: MoveContext): void {
    if (moveCtx.silent) return;
    const { justPlayed, san, end } = moveCtx;
    const sounds: SoundEvent[] = [];
    const prefix = this.botCtrl[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (end) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const boardSoundVolume = sounds ? this.botCtrl.playSound(justPlayed, sounds) : 1;
    if (boardSoundVolume) site.sound.move({ ...moveCtx, volume: boardSoundVolume });
  }
}

type Elapsed = {
  //sum: number;
  //for: Color;
  from?: number;
};
