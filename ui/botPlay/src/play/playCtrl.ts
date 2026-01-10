import { opposite, parseSquare } from 'chessops';
import type { Bot, LocalBridge, Move, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import type { Board } from '../chess';
import { requestBotMove } from './botMove';
import keyboard from './keyboard';
import { initialGround, updateGround } from '../ground';
import { Game } from '../game';
import { prop, toggle, type Toggle } from 'lib';
import { playMoveSounds } from './sound';
import { PromotionCtrl } from 'lib/game/promotion';
import type { WithGround } from 'lib/game/ground';
import { ClockCtrl, type ClockOpts } from 'lib/game/clock/clockCtrl';
import type { TopOrBottom } from 'lib/game';

export interface PlayOpts {
  pref: Pref;
  game: Game;
  bot: Bot;
  bridge: Promise<LocalBridge>;
  redraw: () => void;
  save: (game: Game) => void;
  close: () => void;
  rematch: () => void;
}

export default class PlayCtrl {
  game: Game; // the entire game with moves and ending info
  board: Board; // the state of the board being displayed on `ground`
  clock?: ClockCtrl;
  promotion: PromotionCtrl;
  menu: Toggle;
  flipped: Toggle = toggle(false);
  blindfold: Toggle;
  // will be replaced by view layer
  ground = prop<CgApi | false>(false);
  autoScroll: () => void = () => {};
  constructor(readonly opts: PlayOpts) {
    this.game = opts.game;
    this.board = opts.game.lastBoard();
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.opts.redraw);
    this.menu = toggle(false, opts.redraw);
    this.blindfold = toggle(false, opts.redraw);

    const clk = this.game.clockState();
    if (clk) {
      const clockData = {
        ...this.game.clockConfig!,
        white: clk.white,
        black: clk.black,
        running: !!clk.ticking,
      };
      this.clock = new ClockCtrl(clockData, opts.pref, clk.ticking, this.makeClockOpts());
    }
    keyboard(this);
    setTimeout(this.safelyRequestBotMove, 500);
  }

  isPlaying = () => !this.game.end;

  isOnLastPly = () => this.board.onPly === this.game.ply();

  colorAt = (position: TopOrBottom) => (position === 'bottom' ? this.game.pov : opposite(this.game.pov));

  bottomColor = () => this.colorAt(this.flipped() ? 'top' : 'bottom');

  flip = () => {
    this.flipped.toggle();
    this.withGround(cg => cg.set({ orientation: this.bottomColor() }));
    this.opts.redraw();
  };

  onUserMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, { submit: this.playUserMove })) {
      this.playUserMove(orig, dest);
    }
  };

  onPieceSelect = () => {
    // fast-forward to last position when attempting to move out of turn
    if (this.board.chess.turn !== this.opts.game.pov) this.goToLast();
  };

  onFlag = () => {
    this.game.computeEnd();
    if (this.game.end?.status === 'outoftime') {
      this.recomputeAndSetClock();
      this.updateGround();
      this.opts.redraw();
    }
  };

  goTo = (ply: Ply) => {
    const newPly = Math.max(0, Math.min(this.game.ply(), ply));
    if (newPly === this.board.onPly) return;
    this.board = this.opts.game.copyAtPly(newPly).lastBoard();
    this.updateGround();
    this.opts.redraw();
    this.autoScroll();
    this.safelyRequestBotMove();
  };

  private updateGround = () => {
    this.withGround(cg => cg.set(updateGround(this.game, this.board)));
  };

  goDiff = (plyDiff: number) => this.goTo(this.board.onPly + plyDiff);

  goToLast = () => this.goTo(this.game.ply());

  private playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    const chessMove = normalizeMove(this.board.chess, {
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion,
    });
    const move = this.game.playMoveAtPly(chessMove, this.board.onPly);
    this.afterMove(move);
    this.goTo(this.game.ply());
    this.safelyRequestBotMove();
  };

  private safelyRequestBotMove = async () => {
    const source = await this.opts.bridge;
    if (this.game.computeEnd()) return;
    if (this.game.turn() === this.game.pov) return;
    const sign = () => this.game.pov + this.game.moves.map(m => m.san).join('');
    const before = sign();
    const chessMove = await requestBotMove(source, this.game);
    if (sign() !== before) return console.warn('Bot move ignored due to board state mismatch');
    if (this.game.computeEnd()) return;
    const onLastPly = this.isOnLastPly();
    const move = this.game.playMoveAtPly(chessMove, this.game.ply());
    this.afterMove(move);
    if (onLastPly) {
      this.goTo(this.game.ply());
      this.withGround(cg => cg.playPremove());
    } else {
      this.opts.redraw();
      this.autoScroll();
    }
  };

  private afterMove = (move: Move): void => {
    playMoveSounds(this, move);
    this.opts.save(this.game);
    this.recomputeAndSetClock();
  };

  private makeClockOpts: () => ClockOpts = () => ({
    onFlag: this.onFlag,
    playable: () => true,
    bothPlayersHavePlayed: () => this.game.moves.length > 1,
    hasGoneBerserk: () => false,
    soundColor: this.game.pov,
    nvui: false,
  });

  private recomputeAndSetClock = () => {
    const clk = this.game.clockState();
    if (this.clock && clk) this.clock.setClock(clk);
  };

  private setGround = () => this.withGround(g => g.set(initialGround(this)));

  private withGround: WithGround = f => {
    const g = this.ground();
    return g ? f(g) : undefined;
  };
}
