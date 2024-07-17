import * as co from 'chessops';
import { makeSocket } from './socket';
import { makeFen } from 'chessops/fen';
import type { MoveRootCtrl, Status } from 'game';
import { type GameState, Game } from './game';
import type { RoundSocket, RoundOpts, RoundData } from 'round';
import type { LocalPlayOpts, LocalSetup, Automator, Outcome } from './types';
import type { BotCtrl } from './botCtrl';

export class GameCtrl {
  game: Game;
  viewing?: Game;
  socket: RoundSocket;
  round: MoveRootCtrl;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  roundData: RoundData;
  private automator?: Automator;

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
    this.game = new Game(this.setup.fen);
    this.roundData = this.makeRoundData(this.setup.fen);
    site.pubsub.on('ply', ply => this.jump(ply));
  }

  get chess(): co.Chess {
    return this.game.chess;
  }

  checkGameOver(): { end: boolean; result?: Outcome | 'error'; reason?: string; status?: Status } {
    return this.game.checkGameOver();
  }

  setAutomator(automator: Automator): void {
    this.automator = automator;
  }

  resetToSetup(): void {
    this.botCtrl.whiteUid = this.setup.white;
    this.botCtrl.blackUid = this.setup.black;
    this.resetBoard();
  }

  reset({ white, black, startingFen }: GameState): void {
    this.botCtrl.whiteUid = white;
    this.botCtrl.blackUid = black;
    this.resetBoard(startingFen);
  }

  resetBoard(fen?: string): void {
    if (fen) this.setup.fen = fen;
    this.game = new Game(this.setup.fen);
    this.round.cg?.set({
      fen: this.fen,
      turnColor: this.chess.turn,
      lastMove: undefined,
      movable: { color: this.chess.turn, dests: this.game.cgDests },
    });
    this.resetRound();
    this.updateTurn();
    this.botCtrl.reset();
    this.automator?.onReset?.();
  }

  jump(ply: number): void {
    this.viewing =
      ply === this.game.moves.length ? undefined : new Game(this.setup.fen, this.game.moves.slice(0, ply));
    this.updateTurn(this.viewing);
  }

  move(uci: Uci): boolean {
    if (this.viewing) this.game = this.viewing;
    this.viewing = undefined;
    const game = this.game;
    const { end, result, reason, status, san, move } = game.move(uci);
    const winner = result === 'white' || result === 'black' ? (result as Color) : undefined;
    this.roundData.steps = this.roundData.steps.slice(0, game.ply);
    this.round.apiMove!({
      status,
      uci,
      san,
      fen: game.fen,
      ply: game.ply,
      dests: game.dests,
      threefold: game.isThreefold,
      check: game.chess.isCheck(),
      winner,
    });
    this.updateTurn();
    if (move?.promotion)
      this.round.cg?.setPieces(
        new Map([
          [
            uci.slice(2, 4) as Cg.Key,
            { color: co.opposite(game.chess.turn), role: move.promotion, promoted: true },
          ],
        ]),
      );
    if (end) {
      this.gameOver(result!, reason!, status!);
      this.redraw();
      return false;
    }
    this.redraw();
    if (game === this.game && this.botCtrl[game.chess.turn]) this.botMove();
    return true;
  }

  async botMove(): Promise<void> {
    if (this.automator?.isStopped) return;
    const botMove = await this.botCtrl.move({ fen: this.setup.fen, moves: this.game.moves }, this.chess);
    if (!this.automator?.isStopped) this.move(botMove);
    else {
      const { end, result, reason, status } = this.game.checkGameOver();
      if (end) {
        this.gameOver(result!, reason!, status!);
        this.redraw();
      }
    }
  }

  /*async load(gameId: string): Promise<void> {
    const gameState = await this.gameDb.get(gameId);
    if (gameState) {
      this.setup.fen = gameState.startingFen;
      this.botCtrl.whiteUid = gameState.white;
      this.botCtrl.blackUid = gameState.black;
      this.resetBoard();
      this.game = new Game(gameState);
    }
  }*/

  get isUserTurn(): boolean {
    return !this.botCtrl[this.chess.turn];
  }

  get fen(): string {
    return makeFen(this.chess.toSetup());
  }

  get ply(): number {
    return 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
  }

  private gameOver(result: string, reason: string, status: Status) {
    this.botCtrl.reset();
    setTimeout(() => {
      if (!this.automator?.onGameEnd(result as 'white' | 'black' | 'draw', reason)) {
        const end = {
          status,
          winner: result === 'white' || result === 'black' ? (result as Color) : undefined,
          boosted: false,
        };
        this.round.endWithData?.(end);
      }
      this.redraw();
    });
  }
  private get roundGame(): RoundData['game'] {
    // TODO - figure out way to remove this
    return {
      id: 'synthetic',
      variant: { key: 'standard', name: 'Standard', short: 'Std' },
      speed: 'classical',
      perf: 'classical',
      rated: false,
      fen: this.fen,
      turns: this.ply,
      source: 'local',
      status: { id: 20, name: 'started' },
      player: 'white',
    };
  }

  private resetRound() {
    this.roundData.game.fen = this.fen;
    this.roundData.steps = [{ ply: 0, san: '', uci: '', fen: this.fen }];
    this.roundData.possibleMoves = this.game.dests;
    this.round.ply = 0;
  }

  private updateTurn(game = this.game) {
    this.roundData.game.player = game.turn;
    this.round.cg?.set({ movable: { color: game.turn, dests: game.cgDests } });
  }

  private makeRoundData(fen?: string): RoundData {
    const bottom = !this.setup.white ? 'white' : !this.setup.black ? 'black' : 'white';
    const top = co.opposite(bottom);
    const twoPlayers = !this.setup.white && !this.setup.black;
    const playerName = {
      white: twoPlayers ? 'White player' : 'Player',
      black: twoPlayers ? 'Black player' : 'Player',
    };
    return {
      game: this.roundGame,
      player: this.player(bottom, this.botCtrl[bottom]?.name ?? playerName[bottom]),
      opponent: this.player(top, this.botCtrl[top]?.name ?? playerName[top]),
      pref: this.opts.pref,
      steps: [{ ply: 0, san: '', uci: '', fen: fen ?? co.fen.INITIAL_FEN }],
      takebackable: true,
      moretimeable: true,
      possibleMoves: this.game.dests,
    };
  }

  player(color: Color, name: string): RoundData['player'] {
    // TODO - figure out way to delete this shit
    return {
      color,
      user: {
        id: '', //name.toLowerCase().replace(' ', ''),
        username: name,
        online: true,
        perfs: {},
      },
      id: '',
      //image: this.botCtrl.players[color]?.imageUrl,
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
      onChange: (d: RoundData) => {}, //console.log(d),
    };
  }
}
