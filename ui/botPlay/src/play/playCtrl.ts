import { Chess, Move, parseSquare } from 'chessops';
import { Game, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { makeSanAndPlay, parseSan } from 'chessops/san';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';
import { BotInfo } from 'local';
import { parsePgn } from 'chessops/pgn';

export default class PlayCtrl {
  chess: Chess;
  onPly: number;
  ground?: CgApi;
  constructor(
    readonly pref: Pref,
    readonly game: Game,
    readonly bot: BotInfo,
    readonly redraw: () => void,
    readonly save: (game: Game) => void,
  ) {
    this.chess = Chess.default();
    const pgn = parsePgn(game.sans.join(' '))[0];
    if (pgn) {
      for (const node of pgn.moves.mainline()) {
        const move = parseSan(this.chess, node.san);
        if (!move) break; // Illegal move
        this.chess.play(move);
      }
    }
    this.onPly = this.game.sans.length;
  }

  setGround = (cg: CgApi) => (this.ground = cg);

  isPlaying = () => true;

  onMove = (_orig: Key, _dest: Key) => {};

  onUserMove = (orig: Key, dest: Key) => {
    const move = normalizeMove(this.chess, { from: parseSquare(orig)!, to: parseSquare(dest)! });
    this.addMove(move);
    this.scheduleBotMove();
  };

  private scheduleBotMove = () => {
    setTimeout(this.botMoveNow, 1000);
  };

  private botMoveNow = () => {
    const dests = this.chess.allDests();
    // list all possible moves
    const moves = Array.from(dests.entries()).flatMap(([from, tos]) =>
      Array.from(tos).map(to => ({ from, to })),
    );
    const move = moves[Math.floor(Math.random() * moves.length)];
    this.addMove(move);
  };

  private addMove = (move: Move) => {
    const san = makeSanAndPlay(this.chess, normalizeMove(this.chess, move));
    this.game.sans.push(san);
    this.onPly = this.game.sans.length;
    this.ground?.set({
      fen: makeFen(this.chess.toSetup()),
      check: this.chess.isCheck(),
      turnColor: this.game.sans.length % 2 === 0 ? 'white' : 'black',
      movable: {
        dests: this.isPlaying() ? chessgroundDests(this.chess) : new Map(),
      },
    });
    this.redraw();
    this.save(this.game);
  };
}
