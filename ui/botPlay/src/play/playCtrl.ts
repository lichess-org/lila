import { Move, opposite, parseSquare } from 'chessops';
import { Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { BotInfo, MoveSource } from 'local';
import { addMove, Board, makeBoardAt } from '../chess';
import { requestBotMove } from './botMove';
import keyboard from './keyboard';
import { updateGround } from '../ground';
import { makeFen } from 'chessops/fen';
import { makeEndOf, Game } from '../game';
import { toggle, Toggle } from 'common';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: BotInfo;
  moveSource: Promise<MoveSource>;
  redraw: () => void;
  save: (game: Game) => void;
  close: () => void;
  rematch: () => void;
}

export default class PlayCtrl {
  game: Game;
  board: Board; // the state of the board being displayed
  ground?: CgApi;
  // will be replaced by view layer
  autoScroll: () => void = () => {};
  menu: Toggle;
  flipped: Toggle = toggle(false);
  constructor(readonly opts: PlayOpts) {
    this.game = opts.game;
    this.board = makeBoardAt(opts.game);
    this.menu = toggle(false, opts.redraw);
    keyboard(this);
  }

  setGround = (cg: CgApi) => {
    this.ground = cg;
    this.safelyRequestBotMove();
  };

  isPlaying = () => true;

  lastPly = () => this.game.sans.length;

  isOnLastPly = () => this.board.onPly === this.lastPly();

  bottomColor = () => (this.flipped() ? opposite(this.game.pov) : this.game.pov);

  flip = () => {
    this.flipped.toggle();
    this.ground?.set({ orientation: this.bottomColor() });
    this.opts.redraw();
  };

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
    this.safelyRequestBotMove();
  };

  goDiff = (plyDiff: number) => this.goTo(this.board.onPly + plyDiff);

  goToLast = () => this.goTo(this.lastPly());

  private addMove = (move: Move) => {
    const san = addMove(this.board, move);
    this.game.sans = [...this.game.sans.slice(0, this.board.onPly), san];
    this.game.end = makeEndOf(this.board.chess);
    this.ground?.set(updateGround(this.board));
    this.opts.redraw();
    this.opts.save(this.game);
    this.ground?.playPremove();
    this.autoScroll();
  };

  private safelyRequestBotMove = async () => {
    if (!this.isOnLastPly() || this.game.pov == this.board.chess.turn || this.board.chess.isEnd()) return;
    const source = await this.opts.moveSource;
    const sign = () => this.game.pov + makeFen(this.board.chess.toSetup());
    const before = sign();
    const move = await requestBotMove(source, this.game);
    if (sign() == before) this.addMove(move);
    else console.warn('Bot move ignored due to board state mismatch');
  };
}
