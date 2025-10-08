import { Chess, type Move as ChessMove, opposite } from 'chessops';
import { makeFen, parseFen } from 'chessops/fen';
import { defaultGame, parsePgn, type PgnNodeData, type Game as PgnGame } from 'chessops/pgn';
import type { ClockConfig, SetData as ClockState } from 'lib/game/clock/clockCtrl';
import type { BotKey, GameData, GameEnd, GameId, Move } from './interfaces';
import type { Board } from './chess';
import { makeSan, parseSan } from 'chessops/san';
import { normalizeMove } from 'chessops/chess';
import { randomId } from 'lib/algo';
import { computeClockState } from './clock';

export class Game {
  constructor(readonly data: GameData) {
    this.computeEnd();
  }

  ply = (): Ply => this.moves.length;
  turn = (): Color => (this.ply() % 2 ? 'black' : 'white');

  isClockTicking = (): Color | undefined => (this.end || this.moves.length < 2 ? undefined : this.turn());

  clockState = (): ClockState | undefined => computeClockState(this);

  rewindToPly = (ply: Ply): void => {
    this.data.moves = this.moves.slice(0, ply);
  };

  playMoveAtPly = (chessMove: ChessMove, ply: Ply): Move => {
    this.rewindToPly(ply);
    const chess = this.lastBoard().chess;
    const move: Move = {
      san: makeSan(chess, normalizeMove(chess, chessMove)),
      at: Date.now(),
    };
    this.moves.push(move);
    this.computeEnd();
    return move;
  };

  copyAtPly = (ply: Ply): Game => {
    if (ply >= this.ply()) return this;
    const moves = this.moves.slice(0, ply);
    return new Game({ ...this.data, moves, end: undefined });
  };

  toPgn = (): [PgnGame<PgnNodeData>, Chess] => {
    const headers = new Map<string, string>();
    if (this.data.initialFen) headers.set('FEN', this.data.initialFen);
    const pgn = parsePgn(this.moves.map(m => m.san).join(' '), () => headers)[0] || defaultGame();
    const chess: Chess = this.data.initialFen
      ? parseFen(this.data.initialFen)
          .chain(setup => Chess.fromSetup(setup))
          .unwrap(
            i => i,
            _ => Chess.default(),
          )
      : Chess.default();
    return [pgn, chess];
  };

  lastBoard = (): Board => {
    const [pgn, chess] = this.toPgn();
    const board: Board = { onPly: 0, chess };
    if (!pgn) return board;
    for (const node of pgn.moves.mainline()) {
      const move = parseSan(board.chess, node.san);
      if (!move) {
        // Illegal move
        console.warn('Illegal move', node.san);
        this.rewindToPly(board.onPly);
        break;
      }
      board.chess.play(move);
      board.onPly++;
      board.lastMove = move;
    }
    return board;
  };

  computeEnd = (): GameEnd | undefined => {
    if (this.end) return this.end;
    const chess = this.lastBoard().chess;
    this.data.end = endOnTheBoard(chess);
    if (this.end) return this.end;
    const clock = this.clockState();
    if (!clock) return;
    const flag = (color: Color): GameEnd => ({
      winner: opposite(color),
      status: 'outoftime',
      fen: makeFen(chess.toSetup()),
      at: Date.now(),
    });
    if (clock?.white <= 0) this.data.end = flag('white');
    else if (clock?.black <= 0) this.data.end = flag('black');
    return this.data.end;
  };

  worthResuming = () => this.moves.length > 1 && !this.end;

  get id(): GameId {
    return this.data.id;
  }
  get botKey(): BotKey {
    return this.data.botKey;
  }
  get pov(): Color {
    return this.data.pov;
  }
  get clockConfig(): ClockConfig | undefined {
    return this.data.clockConfig;
  }
  get moves(): Move[] {
    return this.data.moves;
  }
  get end(): GameEnd | undefined {
    return this.data.end;
  }

  static randomId = () => 'b-' + randomId(10);
}

const endOnTheBoard = (chess: Chess): GameEnd | undefined => {
  if (!chess.isEnd()) return;
  return {
    winner: chess.outcome()?.winner,
    status: chess.isCheckmate() ? 'mate' : chess.isStalemate() ? 'stalemate' : 'draw',
    fen: makeFen(chess.toSetup()),
    at: Date.now(),
  };
};
