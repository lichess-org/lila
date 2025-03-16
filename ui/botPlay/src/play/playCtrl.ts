import { Chess, Move, parseSquare } from 'chessops';
import { Game, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeFen } from 'chessops/fen';
import { chessgroundDests, chessgroundMove } from 'chessops/compat';
import { BotInfo } from 'local';
import { parsePgn } from 'chessops/pgn';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: BotInfo;
  redraw: () => void;
  save: (game: Game) => void;
  close: () => void;
}

interface Board {
  onPly: number;
  chess: Chess;
  lastMove?: Move;
}

export default class PlayCtrl {
  game: Game;
  board: Board; // the state of the board being displayed
  ground?: CgApi;
  constructor(readonly opts: PlayOpts) {
    this.game = opts.game;
    this.board = {
      onPly: 0,
      chess: Chess.default(),
    };
    const pgn = parsePgn(opts.game.sans.join(' '))[0];
    if (pgn) {
      for (const node of pgn.moves.mainline()) {
        const move = parseSan(this.board.chess, node.san);
        if (!move) break; // Illegal move
        this.board.chess.play(move);
        this.board.onPly++;
        this.board.lastMove = move;
      }
    }
    this.opts.save(this.game);
  }

  setGround = (cg: CgApi) => (this.ground = cg);

  isPlaying = () => true;

  onMove = (_orig: Key, _dest: Key) => {};

  onUserMove = (orig: Key, dest: Key) => {
    const move = normalizeMove(this.board.chess, { from: parseSquare(orig)!, to: parseSquare(dest)! });
    this.addMove(move);
    this.scheduleBotMove();
  };

  private scheduleBotMove = () => {
    setTimeout(this.botMoveNow, 500);
  };

  private botMoveNow = () => {
    const dests = this.board.chess.allDests();
    // list all possible moves
    const moves = Array.from(dests.entries()).flatMap(([from, tos]) =>
      Array.from(tos).map(to => ({ from, to })),
    );
    const move = moves[Math.floor(Math.random() * moves.length)];
    this.addMove(move);
  };

  private addMove = (move: Move) => {
    const san = makeSanAndPlay(this.board.chess, normalizeMove(this.board.chess, move));
    this.game.sans = this.game.sans.slice(0, this.board.onPly);
    this.game.sans.push(san);
    this.board.onPly = this.game.sans.length;
    this.board.lastMove = move;
    this.ground?.set({
      fen: makeFen(this.board.chess.toSetup()),
      check: this.board.chess.isCheck(),
      turnColor: this.game.sans.length % 2 === 0 ? 'white' : 'black',
      lastMove: this.board.lastMove && chessgroundMove(this.board.lastMove),
      movable: {
        dests: this.isPlaying() ? chessgroundDests(this.board.chess) : new Map(),
      },
    });
    this.opts.redraw();
    this.opts.save(this.game);
    this.ground?.playPremove();
  };
}
