import * as co from 'chessops';
import { makeSocket } from './socket';
import { makeFen } from 'chessops/fen';
import { /*type GameState,*/ type GameStatus, LocalGame } from './localGame';
import type { RoundSocket, RoundOpts, RoundData, RoundController, ClockData } from 'round';
import type { LocalPlayOpts, LocalSetup, Automator, SoundEvent } from './types';
import type { BotCtrl } from './botCtrl';
import { statusOf } from 'game/status';

export class GameCtrl {
  private stopped = true;
  game: LocalGame;
  history?: LocalGame;
  socket: RoundSocket;
  round: RoundController;
  clock?: ClockData;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  roundData: RoundData;
  cancelBotThink?: () => void;
  automator?: Automator;

  constructor(
    readonly opts: LocalPlayOpts,
    readonly botCtrl: BotCtrl,
    readonly redraw: () => void,
  ) {
    this.socket = makeSocket(this);
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
    site.pubsub.on('ply', ply => this.jump(ply));
    ['white', 'black'].forEach(c => this.botCtrl.playSound(c as Color, ['greeting']));
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
    return !this.setup.white ? 'white' : !this.setup.black ? 'black' : 'white';
  }

  get cgOrientation(): Color {
    return this.round?.flip ? co.opposite(this.orientation) : this.orientation;
  }

  start(): void {
    if (this.game.end) return;
    this.stopped = false;
    this.updateTurn();
  }

  async stop(): Promise<void> {
    if (this.isStopped) return;
    this.stopped = true;
  }

  reset(setup: LocalSetup = this.setup): void {
    this.setup = setup;
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
    this.automator?.onReset?.();
    this.syncClock();
    this.resetRound();
  }

  resetClock({ initial, increment }: { initial?: number; increment?: number } = this.setup): void {
    initial ??= this.clock?.initial ?? 0;
    increment ??= this.clock?.increment ?? 0;
    this.clock =
      initial > 0
        ? {
            running: false,
            initial,
            increment,
            white: initial,
            black: initial,
            emerg: 0,
            showTenths: this.opts.pref.clockTenths,
            showBar: true,
            moretime: 0,
          }
        : undefined;

    this.roundData.game.perf = 'classical';
    if (!this.clock) return;

    Object.defineProperty(this.clock, 'running', { get: () => !this.isStopped && this.game.ply > 0 });

    const total = (initial === 0 ? Infinity : initial) + (increment ?? 0) * 40;
    this.roundData.game.perf =
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
    const justPlayed = this.turn;

    const moveResult = this.game.move(uci);
    const { end, san, move } = moveResult;

    const sounds: SoundEvent[] = [];
    const prefix = this.botCtrl[justPlayed] ? 'bot' : 'player';
    if (san.includes('x')) sounds.push(`${prefix}Capture`);
    if (this.chess.isCheck()) sounds.push(`${prefix}Check`);
    if (end) sounds.push(`${prefix}Win`);
    sounds.push(`${prefix}Move`);
    const boardSoundVolume = sounds ? this.botCtrl.playSound(justPlayed, sounds) : 1;

    this.roundData.steps.splice(this.game.ply);

    this.round.apiMove!({ ...moveResult, volume: boardSoundVolume });

    if (move?.promotion)
      this.round.chessground?.setPieces(
        new Map([[uci.slice(2, 4) as Cg.Key, { color: justPlayed, role: move.promotion, promoted: true }]]),
      ); // forever haunted by promotion craplets

    if (end) this.gameOver(moveResult);
    this.redraw();
    return !end;
  }

  async botMove(): Promise<void> {
    this.cancelBotThink?.();
    const [bot, game] = [this.botCtrl[this.turn], this.game];
    if (!bot || this.isStopped || game.end) return;

    const uci = await this.botCtrl.move({ fen: game.startingFen, moves: game.moves.slice() }, this.chess);
    if (uci === '0000') return;

    const thinking = bot.thinking(this.clock?.[this.turn] ?? Infinity);
    if (!this.automator?.noPause)
      await new Promise<void>(r => setTimeout((this.cancelBotThink = r), thinking * 1000));
    else if (this.clock) this.clock[this.turn] -= thinking;

    if (uci !== '0000' && !this.isStopped && game === this.game && this.round.ply === game.ply)
      this.move(uci);
    else setTimeout(() => this.updateTurn(), 200);
  }

  flag(): void {
    this.stop();
    if (this.clock) this.clock[this.turn] = 0;
    this.syncClock();
    Object.freeze(this.game);
    this.gameOver({
      winner: this.pondering,
      status: statusOf('outoftime'),
    });
  }

  private elapsed: Elapsed = { sum: 0, for: 'black' };

  private updateTurn(game = this.game) {
    this.roundData.game.player = game.turn;
    this.round.chessground?.set({ movable: { color: game.turn, dests: game.cgDests } });
    this.syncClock();
    if (this.isLive) this.botMove();
  }

  private syncClock(): void {
    if (!this.clock) return;
    if (this.elapsed.from && this.elapsed.for === (this.isLive ? this.pondering : this.turn))
      this.elapsed.sum += performance.now() - this.elapsed.from;
    this.clock[this.elapsed.for] -= this.elapsed.sum / 1000;
    this.elapsed = { sum: 0, for: this.turn, from: this.isLive ? performance.now() : undefined };
    this.round.clock?.setClock(this.roundData, this.clock.white, this.clock.black);
    if (this.isStopped || !this.isLive) this.round.clock?.stopClock();
  }

  private gameOver(gameStatus: Omit<GameStatus, 'end' | 'turn'>) {
    if (this.clock) this.round.clock?.stopClock();
    if (!this.automator?.onGameOver({ ...gameStatus, end: true, turn: this.turn })) {
      this.round.endWithData?.({ ...gameStatus, boosted: false });
    }
    this.redraw();
  }

  private resetRound() {
    const bottom = this.orientation;
    const top = co.opposite(bottom);
    const twoPlayers = !this.setup.white && !this.setup.black;
    const playerName = {
      white: twoPlayers ? 'White player' : 'Player',
      black: twoPlayers ? 'Black player' : 'Player',
    };
    this.roundData.game.fen = this.fen;
    this.roundData.game.turns = 0;
    this.roundData.steps = [{ ply: 0, san: '', uci: '', fen: this.fen }];
    this.roundData.possibleMoves = this.game.dests;
    this.roundData.player = this.player(bottom, this.botCtrl[bottom]?.name ?? playerName[bottom]);
    this.roundData.opponent = this.player(top, this.botCtrl[top]?.name ?? playerName[top]);
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

  private makeRoundData(setup: LocalSetup): void {
    this.roundData = Object.defineProperty(
      {
        game: {
          id: 'synthetic',
          variant: { key: 'standard', name: 'Standard', short: 'Std' },
          speed: 'classical',
          perf: 'unlimited',
          rated: false,
          fen: this.fen,
          turns: this.ply,
          source: 'local',
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
    // TODO - figure out way to delete this shit
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
      data: this.roundData,
      i18n: this.opts.i18n,
      local: this.socket,
      onChange: (d: RoundData) => {
        if (this.round.ply > 0) return;
        this.round.chessground?.set({
          fen: this.fen,
          turnColor: this.turn,
          lastMove: undefined,
          check: this.chess.isCheck(),
          movable: { color: this.turn, dests: this.game.cgDests },
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
