import { Move, parseSquare } from 'chessops';
import { Game, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { BotInfo } from 'local';
import { addMove, Board, makeBoardAt } from './chess';
import { scheduleBotMove } from './botMove';
import keyboard from './keyboard';
import { updateGround } from './view/ground';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: BotInfo;
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

  setGround = (cg: CgApi) => (this.ground = cg);

  isPlaying = () => true;

  onMove = (_orig: Key, _dest: Key) => {};

  onUserMove = (orig: Key, dest: Key) => {
    const move = normalizeMove(this.board.chess, { from: parseSquare(orig)!, to: parseSquare(dest)! });
    this.addMove(move);
    scheduleBotMove(this.board, this.addMove);
  };

  goTo = (ply: Ply) => {
    const newPly = Math.max(0, Math.min(this.game.sans.length, ply));
    this.board = makeBoardAt(this.opts.game, newPly);
    this.ground?.set(updateGround(this.board, this.game));
    this.opts.redraw();
    this.autoScroll();
  };

  goDiff = (plyDiff: number) => this.goTo(this.board.onPly + plyDiff);

  private addMove = (move: Move) => {
    addMove(this.board, this.game, move);
    this.ground?.set(updateGround(this.board, this.game));
    this.opts.redraw();
    this.opts.save(this.game);
    this.ground?.playPremove();
    this.autoScroll();
  };
}
