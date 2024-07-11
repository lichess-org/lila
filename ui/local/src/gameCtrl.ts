import * as co from 'chessops';
import { makeSocket } from './socket';
import { GameDb } from './gameDb';
import { makeFen } from 'chessops/fen';
import { normalizeMove } from 'chessops/chess';
import { makeSanAndPlay } from 'chessops/san';
import type { MoveRootCtrl, Status } from 'game';
import { statusOf } from 'game/status';
import type { RoundSocket, RoundOpts, RoundData } from 'round';
import type { LocalPlayOpts, LocalSetup, Automator, Outcome } from './types';
import type { BotCtrl } from './botCtrl';

export interface GameState {
  startingFen: string;
  moves: Uci[];
  threefoldFens?: Map<string, number>;
  fiftyMovePly?: number;
  white: string | undefined;
  black: string | undefined;
}

export class GameCtrl {
  chess: co.Chess = co.Chess.default();
  socket: RoundSocket;
  fiftyMovePly: number = 0;
  threefoldFens: Map<string, number> = new Map();
  round: MoveRootCtrl;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  moves: Uci[] = [];
  roundData: RoundData;
  gameDb: GameDb = new GameDb();
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
    this.roundData = this.makeRoundData(this.setup.fen);
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
    this.fiftyMovePly = 0;
    this.moves = [];
    this.threefoldFens.clear();
    this.chess = co.Chess.fromSetup(
      co.fen.parseFen(fen ?? this.setup.fen ?? co.fen.INITIAL_FEN).unwrap(),
    ).unwrap();
    this.round.cg?.set({
      fen: this.fen,
      turnColor: this.chess.turn,
      lastMove: undefined,
      movable: { color: this.chess.turn, dests: this.cgDests },
    });
    this.updateRound();
    this.updateTurn();
    this.botCtrl.reset();
    this.automator?.onReset?.();
  }

  checkGameOver(userEnd?: 'whiteResign' | 'blackResign' | 'mutualDraw'): {
    end: boolean;
    result?: Outcome | 'error';
    reason?: string;
    status?: Status;
  } {
    let status = statusOf('started');
    let result: Outcome | 'error' = 'draw',
      reason = userEnd ?? 'checkmate';
    if (this.chess.isCheckmate()) [result, status] = [co.opposite(this.chess.turn), statusOf('mate')];
    else if (this.chess.isInsufficientMaterial()) [reason, status] = ['insufficient', statusOf('draw')];
    else if (this.chess.isStalemate()) [reason, status] = ['stalemate', statusOf('stalemate')];
    else if (this.fifty()) [reason, status] = ['fifty', statusOf('draw')];
    else if (this.isThreefold) [reason, status] = ['threefold', statusOf('draw')];
    else if (userEnd) {
      if (userEnd !== 'mutualDraw') [reason, status] = ['resign', statusOf('resign')];
      if (userEnd === 'whiteResign') result = 'black';
      else if (userEnd === 'blackResign') result = 'white';
    } else return { end: false };
    // needs outoftime
    return { end: true, result, reason, status };
  }

  move(uci: Uci): boolean {
    let move: co.NormalMove | undefined;
    try {
      const bareMove = co.parseUci(uci) as co.NormalMove;
      move = { ...(normalizeMove(this.chess, bareMove) as co.NormalMove), promotion: bareMove.promotion };
    } catch (e) {
      console.log(`error parsing ${uci}`, e);
    }
    if (!move || !this.chess.isLegal(move)) {
      this.gameOver(
        'error',
        `${this.botCtrl[this.chess.turn]?.name} made illegal move ${uci} at ${makeFen(this.chess.toSetup())}`,
        { name: 'aborted', id: 20 },
      );
      return false;
    }
    uci = co.makeUci(move); // fix e1h1/e8h8 nonsense
    this.moves.push(uci);
    const san = makeSanAndPlay(this.chess, move);
    this.fifty(move);
    this.updateThreefold();
    //this.socket.receive('move', { uci, san, fen: this.fen, ply: this.ply, dests: this.dests });
    const { end, result, reason, status } = this.checkGameOver();
    const winner = result === 'white' || result === 'black' ? (result as Color) : undefined;
    this.round.apiMove!({
      status,
      uci,
      san,
      fen: this.fen,
      ply: this.ply,
      dests: this.dests,
      threefold: this.isThreefold,
      check: this.chess.isCheck(),
      winner,
    });
    this.updateTurn();
    if (move.promotion)
      this.round.cg?.setPieces(
        new Map([
          [
            uci.slice(2, 4) as Cg.Key,
            { color: co.opposite(this.chess.turn), role: move.promotion, promoted: true },
          ],
        ]),
      );
    if (end) {
      this.gameOver(result!, reason!, status!);
      this.redraw();
      return false;
    }
    this.redraw();
    if (this.botCtrl[this.chess.turn]) this.botMove();
    return true;
  }

  async botMove(): Promise<void> {
    if (this.automator?.isStopped) return;
    const botMove = await this.botCtrl.move({ fen: this.setup.fen, moves: this.moves }, this.chess);
    if (!this.automator?.isStopped) this.move(botMove);
    else {
      const { end, result, reason, status } = this.checkGameOver();
      if (end) {
        this.gameOver(result!, reason!, status!);
        this.redraw();
      }
    }
  }

  async load(gameId: string): Promise<void> {
    const gameState = await this.gameDb.get(gameId);
    if (gameState) {
      this.setup.fen = gameState.startingFen;
      this.moves = gameState.moves;
      this.threefoldFens = gameState.threefoldFens ?? new Map();
      this.fiftyMovePly = gameState.fiftyMovePly ?? 0;
      this.botCtrl.whiteUid = gameState.white;
      this.botCtrl.blackUid = gameState.black;
      this.resetBoard();
      gameState.moves.forEach(move => this.chess.play(co.parseUci(move)!));
    }
  }

  player(color: Color, name: string): RoundData['player'] {
    // TODO - figure out way to delete this shit
    return {
      color,
      user: {
        id: name.toLowerCase().replace(' ', ''),
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

  private fifty(move?: co.Move): boolean {
    if (move)
      if (
        !('from' in move) ||
        this.chess.board.getRole(move.from) === 'pawn' ||
        this.chess.board.get(move.to)
      )
        this.fiftyMovePly = 0;
      else this.fiftyMovePly++;
    return this.fiftyMovePly >= 100;
  }

  private updateThreefold(): boolean {
    const boardFen = this.fen.split('-')[0];
    let fenCount = this.threefoldFens.get(boardFen) ?? 0;
    this.threefoldFens.set(boardFen, ++fenCount);
    return fenCount >= 3; // TODO fixme
  }

  private get isThreefold() {
    return (this.threefoldFens.get(this.fen.split('-')[0]) ?? 0) >= 3; // TODO fixme
  }

  /*isPromotion(move: co.Move) {
    return (
      'from' in move &&
      co.squareRank(move.to) === (this.chess.turn === 'white' ? 7 : 0) &&
      this.chess.board.getRole(move.from) === 'pawn'
    );
  }*/

  /*get isBotTurn() {
    return !!this.botCtrl[this.chess.turn];
  }*/

  private get dests() {
    const dests: { [from: string]: string } = {};
    [...this.chess.allDests()]
      .filter(([, to]) => !to.isEmpty())
      .forEach(([s, ds]) => (dests[co.makeSquare(s)] = [...ds].map(co.makeSquare).join('')));
    return dests;
  }

  private get cgDests() {
    const dec = new Map();
    const dests = this.dests;
    if (!dests) return dec;
    for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as Cg.Key[]);
    return dec;
  }

  private get game(): RoundData['game'] {
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

  private updateRound() {
    this.roundData.game.fen = this.fen;
    this.roundData.possibleMoves = this.dests;
    this.round.reset?.(this.fen ?? co.fen.INITIAL_FEN);
  }

  private updateTurn() {
    this.roundData.game.player = this.chess.turn;
    this.round.cg?.set({ movable: { color: this.chess.turn, dests: this.cgDests } });
  }

  private makeRoundData(fen?: string): RoundData {
    const bottom = !this.setup.white ? 'white' : !this.setup.black ? 'black' : 'white';
    const top = co.opposite(bottom);
    console.log(this.opts.pref);
    return {
      game: this.game,
      player: this.player(bottom, this.botCtrl[bottom]?.name ?? 'Player'),
      opponent: this.player(top, this.botCtrl[top]?.name ?? 'Player'),
      pref: this.opts.pref,
      steps: [{ ply: 0, san: '', uci: '', fen: fen ?? co.fen.INITIAL_FEN }],
      takebackable: true,
      moretimeable: true,
      possibleMoves: this.dests,
    };
  }

  private async saveGameState() {
    const gameState: GameState = {
      startingFen: this.setup.fen ?? co.fen.INITIAL_FEN,
      moves: this.moves,
      threefoldFens: this.threefoldFens,
      fiftyMovePly: this.fiftyMovePly,
      white: this.botCtrl.white?.name,
      black: this.botCtrl.black?.name,
    };
    await this.gameDb.save(this.roundData.game.id, gameState);
  }
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: any } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: co.charToRole(uci.slice(4)) };
}
