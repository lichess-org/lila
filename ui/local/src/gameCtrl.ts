import * as co from 'chessops';
import { makeFakeLila } from './fakeLila';
import { makeFen } from 'chessops/fen';
import { type MoveResult, type GameStatus, LocalGame } from './localGame';
import type { LocalLila, RoundOpts, RoundData, RoundController, ClockData } from 'round';
import type { Source } from 'game';
import type { LocalPlayOpts, LocalSetup, Automator, SoundEvent } from './types';
import type { BotCtrl } from './botCtrl';
import { deepFreeze } from 'common';
import { statusOf } from 'game/status';

export class GameCtrl {
  private stopped = true;
  game: LocalGame;
  history?: LocalGame;
  stub: LocalLila;
  round: RoundController;
  clock?: ClockData;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  data: RoundData;
  cancelBotThink?: () => void;
  dev?: Automator;

  constructor(
    readonly opts: LocalPlayOpts,
    readonly botCtrl: BotCtrl,
    readonly redraw: () => void,
    readonly source: Source = 'local',
  ) {
    this.stub = makeFakeLila(this);
    this.i18n = opts.i18n;
    if (opts.setup)
      this.setup = Object.fromEntries(
        Object.entries({ ...opts.setup }).map(([k, v]) => [k, v === null ? undefined : v]),
      );
    else this.setup = {};
    this.setup.fen ??= co.fen.INITIAL_FEN;
    this.game = new LocalGame(this.setup.fen);
    this.makeRoundData(this.setup);
    this.resetClock();
    this.autoStart();
    site.pubsub.on('ply', ply => this.jump(ply));
    //['white', 'black'].forEach(c => this.botCtrl.playSound(c as Color, ['greeting']));
    /*setTimeout(() => {
      if (!this.isUserTurn) this.start();
    }, 1000);*/
  }

  get chess(): co.Chess {
    return this.game.chess;
  }

  get turn(): Color {
    return this.chess.turn;
  }

  get pondering(): Color {
    return co.opposite(this.turn);
  }

  get status(): GameStatus {
    return this.game.status;
  }

  get isUserTurn(): boolean {
    return !this.botCtrl[this.turn];
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

  get orientation(): Color {
    return /*this.dev ? 'white' :*/ !this.setup.white ? 'white' : !this.setup.black ? 'black' : 'white';
  }

  get cgOrientation(): Color {
    return this.round?.flip ? co.opposite(this.orientation) : this.orientation;
  }

  start(): void {
    this.stopped = false;
    setTimeout(() => !this.game.end && this.updateTurn());
  }

  autoStart(): void {
    ['white', 'black'].forEach(c => this.botCtrl.playSound(c as Color, ['greeting']));
    setTimeout(() => !this.dev && !this.isUserTurn && this.start(), 600);
  }

  async stop(): Promise<void> {
    if (this.isStopped) return;
    this.stopped = true;
    this.cancelBotThink?.();
  }

  reset(setup: LocalSetup = this.setup): void {
    this.setup = { ...this.setup, ...setup };
    this.botCtrl.whiteUid = this.setup.white;
    this.botCtrl.blackUid = this.setup.black;
    this.resetBoard();
  }

  resetBoard(fen?: string): void {
    this.game = new LocalGame(fen ?? this.setup.fen ?? co.fen.INITIAL_FEN);
    this.stop();
    this.botCtrl.reset();
    this.updateTurn();
    this.resetClock();
    this.syncClock();
    this.resetRound();
    this.autoStart();
  }

  resetClock({ initial, increment }: { initial?: number; increment?: number } = this.setup): void {
    initial ??= this.clock?.initial ?? 0;
    increment ??= this.clock?.increment ?? 0;
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
      { get: () => !this.isStopped && this.game.ply > 0 },
    );

    const total = initial + (increment ?? 0) * 40;
    this.data.game.speed = this.data.game.perf =
      total < 30
        ? 'ultraBullet'
        : total < 180
        ? 'bullet'
        : total < 480
        ? 'blitz'
        : total < 1500
        ? 'rapid'
        : 'classical';
  }

  jump(ply: number): void {
    this.history =
      ply < this.game.moves.length ? new LocalGame(this.setup.fen, this.game.moves.slice(0, ply)) : undefined;
    if (this.history) return this.updateTurn(this.history);
    this.updateTurn();
  }

  move(uci: Uci): boolean {
    if (this.history) this.game = this.history;
    this.history = undefined;
    this.stopped = false;

    const moveResult = this.game.move({ uci, clock: this.clock });
    const { end, move, justPlayed } = moveResult;

    this.data.steps.splice(this.game.ply);
    this.dev?.preMove?.(moveResult);
    this.playSounds(moveResult);
    this.round.apiMove!(moveResult);

    if (move?.promotion)
      this.round.chessground?.setPieces(
        new Map([[uci.slice(2, 4) as Cg.Key, { color: justPlayed, role: move.promotion, promoted: true }]]),
      );

    if (end) this.gameOver(moveResult);
    if (this.clock?.increment && this.clock?.initial) {
      this.clock[justPlayed] += this.clock.increment;
      this.syncClock();
    }
    this.redraw();
    return !end;
  }

  async botMove(): Promise<void> {
    this.cancelBotThink?.();
    const [bot, game] = [this.botCtrl[this.turn], this.game];
    if (!bot || this.isStopped || game.end) return;
    const move = await this.botCtrl.move({
      pos: { fen: game.initialFen, moves: game.moves.map(x => x.uci) },
      chess: this.chess,
      secondsRemaining: this.clock?.[this.turn],
      initial: this.clock?.initial,
      increment: this.clock?.increment,
    });
    if (!move) return;

    if (!this.dev?.think(move.time))
      await new Promise<void>(r => setTimeout((this.cancelBotThink = r), move.time * 1000));

    if (!this.isStopped && game === this.game && this.round.ply === game.ply) this.move(move.uci);
    else setTimeout(() => this.updateTurn(), 200);
  }

  flag(): void {
    if (this.clock) this.clock[this.turn] = 0;
    this.gameOver({ winner: this.pondering, status: statusOf('outoftime') });
    this.syncClock();
  }

  resign(): void {
    this.gameOver({ winner: this.pondering, status: statusOf('resign') });
  }

  abort(): void {
    this.gameOver({ winner: undefined, status: statusOf('aborted') });
  }

  draw(): void {
    this.gameOver({ winner: undefined, status: statusOf('draw') });
  }

  private elapsed: Elapsed = { sum: 0, for: 'black' };

  private updateTurn(game = this.game) {
    this.data.game.player = game.turn;
    this.round.chessground?.set({ movable: { color: game.turn, dests: game.cgDests } });
    if (this.clock) this.clock = { ...this.clock, ...game.clock };
    this.syncClock();
    if (this.isLive) this.botMove();
  }

  private syncClock(): void {
    if (!this.clock) return;
    if (this.elapsed.from && this.elapsed.for === (this.isLive ? this.pondering : this.turn))
      this.elapsed.sum += performance.now() - this.elapsed.from;
    this.clock[this.elapsed.for] -= this.elapsed.sum / 1000;
    this.elapsed = {
      sum: 0,
      for: this.turn,
      from: this.isLive && this.ply > 0 ? performance.now() : undefined,
    };
    this.round.clock?.setClock(this.data, this.clock.white, this.clock.black);
    if (this.isStopped || !this.isLive) this.round.clock?.stopClock();
  }

  private gameOver(gameStatus: Omit<GameStatus, 'end' | 'turn'>) {
    this.game.finish(gameStatus);
    this.stop();
    if (this.clock) this.round.clock?.stopClock();
    if (!this.dev?.onGameOver({ ...gameStatus, end: true, turn: this.turn })) {
      this.reset();
      this.round.endWithData?.({ ...gameStatus, boosted: false });
    }
    this.redraw();
  }

  private playSounds(moveResult: MoveResult): void {
    if (moveResult.silent) return;
    const { justPlayed, san, end } = moveResult;
    const sounds: SoundEvent[] = [];
    const prefix = this.botCtrl[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (end) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const boardSoundVolume = sounds ? this.botCtrl.playSound(justPlayed, sounds) : 1;
    if (boardSoundVolume) site.sound.move({ ...moveResult, volume: boardSoundVolume });
  }

  private resetRound() {
    const bottom = this.orientation;
    const top = co.opposite(bottom);
    const twoPlayers = !this.setup.white && !this.setup.black;
    const playerName = {
      white: twoPlayers ? 'White player' : 'Player',
      black: twoPlayers ? 'Black player' : 'Player',
    };
    this.data.game.fen = this.fen;
    this.data.game.turns = 0;
    this.data.game.status = { id: 20, name: 'started' };
    this.data.steps = [{ ply: 0, san: '', uci: '', fen: this.fen }];
    this.data.possibleMoves = this.game.dests;
    this.data.player = this.player(bottom, this.botCtrl[bottom]?.name ?? playerName[bottom]);
    this.data.opponent = this.player(top, this.botCtrl[top]?.name ?? playerName[top]);
    if (this.round) this.round.ply = 0;
    this.round?.chessground?.set({
      fen: this.fen,
      turnColor: this.turn,
      lastMove: undefined,
      check: this.chess.isCheck(),
      movable: { color: this.turn, dests: this.game.cgDests },
      orientation: bottom,
    });
  }

  makeRoundData(setup: LocalSetup): void {
    this.data = Object.defineProperty(
      {
        game: {
          id: 'synthetic',
          variant: { key: 'standard', name: 'Standard', short: 'Std' },
          speed: 'classical',
          perf: 'unlimited',
          rated: false,
          fen: this.fen,
          turns: this.ply,
          source: this.source,
          status: { id: 20, name: 'started' },
          player: this.ply % 2 ? 'black' : 'white',
        },
        player: this.player('white', ''),
        opponent: this.player('black', ''),
        pref: this.opts.pref,
        steps: [{ ply: 0, san: '', uci: '', fen: setup.fen ?? co.fen.INITIAL_FEN }],
        takebackable: false,
        moretimeable: false,
        possibleMoves: this.game.dests,
      },
      'clock',
      { get: () => this.clock },
    );
    this.resetRound();
  }

  private player(color: Color, name: string): RoundData['player'] {
    return {
      color,
      user: {
        id: '',
        username: name,
        online: true,
        perfs: {},
      },
      id: '',
      isGone: false,
      name,
      onGame: true,
      version: 0,
    };
  }

  get roundOpts(): RoundOpts {
    return {
      data: this.data,
      i18n: this.opts.i18n,
      local: this.stub,
      onChange: (d: RoundData) => {
        if (this.round.ply > 0) return;
        this.round.chessground?.set({
          fen: this.fen,
          turnColor: this.turn,
          lastMove: undefined,
          check: this.chess.isCheck(),
          movable: {
            color: this.turn,
            dests: this.game.cgDests,
          },
          events: {
            move: undefined,
          },
          orientation: this.orientation,
        });
      },
    };
  }
}

type Elapsed = {
  sum: number;
  for: Color;
  from?: number;
};
