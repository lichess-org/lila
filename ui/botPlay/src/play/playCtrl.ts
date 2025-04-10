import { Move as ChessMove, opposite, parseSquare } from 'chessops';
import { LocalBridge, Pref } from '../interfaces';
import { normalizeMove } from 'chessops/chess';
import { type BotInfo } from 'lib/bot/types';
import { addMove, Board, makeBoardAt } from '../chess';
import { requestBotMove } from './botMove';
import keyboard from './keyboard';
import { initialGround, updateGround } from '../ground';
import { makeFen } from 'chessops/fen';
import { makeEndOf, Game, Move, computeClockState, isClockTicking } from '../game';
import { prop, toggle, Toggle } from 'lib';
import { playMoveSounds } from './sound';
import { PromotionCtrl } from 'lib/game/promotion';
import type { WithGround } from 'lib/game/ground';
import { ClockCtrl, ClockOpts } from 'lib/game/clock/clockCtrl';
import { TopOrBottom } from 'lib/game/game';

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
    this.board = makeBoardAt(opts.game);
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.opts.redraw);
    this.menu = toggle(false, opts.redraw);
    this.blindfold = toggle(false, opts.redraw);

    const clk = computeClockState(this.game);
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

  lastPly = () => this.game.moves.length;

  isOnLastPly = () => this.board.onPly === this.lastPly();

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

  private playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    if (!this.isOnLastPly()) {
      // allow branching out from anywhere
      this.game.moves = this.game.moves.slice(0, this.board.onPly);
    }
    const move = normalizeMove(this.board.chess, {
      from: parseSquare(orig)!,
      to: parseSquare(dest)!,
      promotion,
    });
    this.addMove(move);
    this.safelyRequestBotMove();
  };

  onPieceSelect = () => {
    // fast-forward to last position when attempting to move out of turn
    if (this.board.chess.turn !== this.opts.game.pov) this.goToLast();
  };

  onFlag = () => {
    const ticking = isClockTicking(this.game);
    if (ticking) {
      this.game.end = {
        winner: opposite(ticking),
        status: 'outoftime',
        fen: makeFen(makeBoardAt(this.game).chess.toSetup()),
      };
      this.recomputeAndSetClock();
      this.opts.redraw();
    }
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

  private addMove = (chessMove: ChessMove) => {
    const move: Move = {
      san: addMove(this.board, chessMove),
      at: Date.now(),
    };
    this.game.moves = [...this.game.moves.slice(0, this.board.onPly), move];
    this.game.end = makeEndOf(this.board.chess);
    this.withGround(cg => cg.set(updateGround(this.board)));
    this.recomputeAndSetClock();
    this.opts.redraw();
    this.opts.save(this.game);
    this.autoScroll();
    playMoveSounds(this, move);
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

  private makeClockOpts: () => ClockOpts = () => ({
    onFlag: this.onFlag,
    playable: () => true,
    bothPlayersHavePlayed: () => this.game.moves.length > 1,
    hasGoneBerserk: () => false,
    soundColor: this.game.pov,
    nvui: false,
  });

  private recomputeAndSetClock = () => {
    const clk = computeClockState(this.game);
    if (this.clock && clk) this.clock.setClock(clk);
  };

  private setGround = () => this.withGround(g => g.set(initialGround(this)));

  private withGround: WithGround = f => {
    const g = this.ground();
    return g ? f(g) : undefined;
  };
}
