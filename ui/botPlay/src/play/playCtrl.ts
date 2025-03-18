import { Move, opposite, parseSquare } from 'chessops';
import { LocalBridge, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { BotInfo } from 'local';
import { addMove, Board, makeBoardAt } from '../chess';
import { requestBotMove } from './botMove';
import keyboard from './keyboard';
import { initialGround, updateGround } from '../ground';
import { makeFen } from 'chessops/fen';
import { makeEndOf, Game } from '../game';
import { prop, toggle, Toggle } from 'common';
import { playMoveSounds } from './sound';
import { PromotionCtrl } from 'chess/promotion';
import type { WithGround } from 'chess/ground';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: BotInfo;
  bridge: Promise<LocalBridge>;
  redraw: () => void;
  save: (game: Game) => void;
  close: () => void;
  rematch: () => void;
}

export default class PlayCtrl {
  game: Game;
  board: Board; // the state of the board being displayed
  promotion: PromotionCtrl;
  menu: Toggle;
  flipped: Toggle = toggle(false);
  blindfold: Toggle;
  // will be replaced by view layer
  ground = prop<CgApi | false>(false);
  autoScroll: () => void = () => {};
  constructor(readonly opts: PlayOpts) {
    this.game = opts.game;
    this.board = makeBoardAt(opts.game);
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.opts.redraw);
    this.menu = toggle(false, opts.redraw);
    this.blindfold = toggle(false, opts.redraw);
    keyboard(this);
    setTimeout(this.safelyRequestBotMove, 500);
  }

  isPlaying = () => !this.game.end;

  lastPly = () => this.game.sans.length;

  isOnLastPly = () => this.board.onPly === this.lastPly();

  bottomColor = () => (this.flipped() ? opposite(this.game.pov) : this.game.pov);

  flip = () => {
    this.flipped.toggle();
    this.withGround(cg => cg.set({ orientation: this.bottomColor() }));
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
    this.withGround(cg => cg.set(updateGround(this.board)));
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
    this.withGround(cg => cg.set(updateGround(this.board)));
    this.opts.redraw();
    this.opts.save(this.game);
    this.autoScroll();
    playMoveSounds(this, san);
    this.withGround(cg => cg.playPremove());
  };

  private safelyRequestBotMove = async () => {
    if (!this.isOnLastPly() || this.game.pov == this.board.chess.turn || this.board.chess.isEnd()) return;
    const source = await this.opts.bridge;
    const sign = () => this.game.pov + makeFen(this.board.chess.toSetup());
    const before = sign();
    const move = await requestBotMove(source, this.game);
    if (sign() == before) this.addMove(move);
    else console.warn('Bot move ignored due to board state mismatch');
  };

  private setGround = () => this.withGround(g => g.set(initialGround(this)));

  private withGround: WithGround = f => {
    const g = this.ground();
    return g ? f(g) : undefined;
  };
}
