import { Move, parseSquare } from 'chessops';
import { Game, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { BotCtrl, BotInfo } from 'local';
import { addMove, Board, makeBoardAt } from './chess';
import { requestBotMove } from './botMove';
import keyboard from './keyboard';
import { updateGround } from './view/ground';
import { makeFen } from 'chessops/fen';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: BotInfo;
  bots: Promise<BotCtrl>;
  redraw: () => void;
  save: (game: Game) => void;
  close: () => void;
}

export default class PlayCtrl {
  game: Game;
  board: Board; // the state of the board being displayed
  ground?: CgApi;
  // will be replaced by view layer
  autoScroll: () => void = () => {};
  constructor(readonly opts: PlayOpts) {
    this.game = opts.game;
    this.board = makeBoardAt(opts.game, opts.game.sans.length);
    this.opts.save(this.game);
    keyboard(this);
  }

  setGround = (cg: CgApi) => {
    this.ground = cg;
    this.safelyRequestBotMove();
  };

  isPlaying = () => true;

  lastPly = () => this.game.sans.length;

  isOnLastPly = () => this.board.onPly === this.lastPly();

  onUserMove = (orig: Key, dest: Key) => {
    if (!this.isOnLastPly()) {
      // allow branching out from anywhere
      this.game.sans = this.game.sans.slice(0, this.board.onPly);
    }
    const move = normalizeMove(this.board.chess, { from: parseSquare(orig)!, to: parseSquare(dest)! });
    this.addMove(move);
    this.safelyRequestBotMove();
  };

  onPieceSelect = () => {
    // fast-forward to last position when attempting to move out of turn
    if (this.board.chess.turn !== this.opts.game.pov) this.goToLast();
  };

  goTo = (ply: Ply) => {
    const newPly = Math.max(0, Math.min(this.lastPly(), ply));
    if (newPly === this.board.onPly) return;
    this.board = makeBoardAt(this.opts.game, newPly);
    this.ground?.set(updateGround(this.board));
    this.opts.redraw();
    this.autoScroll();
  };

  goDiff = (plyDiff: number) => this.goTo(this.board.onPly + plyDiff);

  goToLast = () => this.goTo(this.lastPly());

  private addMove = (move: Move) => {
    const san = addMove(this.board, move);
    this.game.sans = [...this.game.sans.slice(0, this.board.onPly), san];
    this.ground?.set(updateGround(this.board));
    this.opts.redraw();
    this.opts.save(this.game);
    this.ground?.playPremove();
    this.autoScroll();
  };

  private safelyRequestBotMove = async () => {
    this.goToLast();
    if (this.board.isEnd || this.game.pov == this.board.chess.turn) return;
    const bots = await this.opts.bots;
    const preState = makeFen(this.board.chess.toSetup());
    const move = await requestBotMove(bots, this.game);
    this.goToLast();
    const postState = makeFen(this.board.chess.toSetup());
    if (preState === postState) this.addMove(move);
    else {
      console.warn('Bot move ignored due to board state mismatch');
    }
  };
}
