import type { Api as CgApi } from '@lichess-org/chessground/api';
import type { Config as CgConfig } from '@lichess-org/chessground/config';
import { uciToMove } from '@lichess-org/chessground/util';
import { Chess, parseUci } from 'chessops';
import { chessgroundDests } from 'chessops/compat';
import { makeFen, parseFen } from 'chessops/fen';

import { prop, type Prop } from 'lib';
import { PromotionCtrl } from 'lib/game/promotion';
import type { PuzPrefs } from 'lib/puz/interfaces';
import { sound } from 'lib/puz/util';
import { makeConfig as makeCgConfig } from 'lib/puz/view/chessground';

import type { GuessPlayer, GuessPosition, GuessResult, Phase, PuzzleGuessOpts } from './interfaces';
import * as xhr from './xhr';

export default class PuzzleGuessCtrl {
  pref: PuzPrefs;
  position?: GuessPosition;
  player?: GuessPlayer;
  phase: Phase = 'guess';
  result?: GuessResult;
  solveFailed = false;
  loading = false;
  noMorePositions = false;
  replaying = false;

  private pos!: Chess;
  private solution: Uci[] = [];
  private moveIndex = 0; // moves played from the shown fen
  private lastUci?: Uci;

  ground: Prop<CgApi | false> = prop<CgApi | false>(false);
  promotion: PromotionCtrl;

  constructor(
    opts: PuzzleGuessOpts,
    readonly redraw: () => void,
  ) {
    this.pref = opts.pref;
    this.player = opts.player;
    if (opts.position) this.setPosition(opts.position);
    else this.noMorePositions = true;
    this.promotion = new PromotionCtrl(this.withGround, () => this.setGround(), this.redraw);
  }

  private setPosition = (position: GuessPosition): void => {
    this.position = position;
    this.pos = Chess.fromSetup(parseFen(position.fen).unwrap()).unwrap();
    this.phase = 'guess';
    this.result = undefined;
    this.solveFailed = false;
    this.replaying = false;
    this.solution = [];
    this.moveIndex = 0;
    this.lastUci = undefined;
  };

  withGround = <A>(f: (cg: CgApi) => A): A | undefined => {
    const g = this.ground();
    return g ? f(g) : undefined;
  };

  cgOpts = (): CgConfig => {
    const canMove = this.phase === 'solve' && !this.replaying && this.pos.turn === this.position?.color;
    return {
      fen: makeFen(this.pos.toSetup()),
      orientation: this.position?.color,
      turnColor: this.pos.turn,
      check: this.pos.isCheck(),
      lastMove: this.lastUci ? uciToMove(this.lastUci) : undefined,
      movable: {
        color: this.position?.color,
        dests: canMove ? chessgroundDests(this.pos) : undefined,
      },
    };
  };

  cgConfig = (): CgConfig => makeCgConfig(this.cgOpts(), this.pref, this.userMove);

  setGround = (): void => {
    this.withGround(g => g.set(this.cgOpts()));
  };

  guess = (isPuzzle: boolean): void => {
    if (this.phase !== 'guess' || !this.position || this.loading) return;
    this.loading = true;
    this.redraw();
    xhr.guess(this.position.id, isPuzzle).then(result => {
      this.loading = false;
      this.result = result;
      if (result.correct) sound.good();
      else sound.wrong();
      if (result.finished) this.phase = 'done';
      else {
        // a correctly identified puzzle: now prove it
        this.phase = 'solve';
        this.solution = result.solution || [];
        this.setGround();
      }
      this.redraw();
    });
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, { submit: this.playUserMove })) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    this.promotion.cancel();
    const uci = `${orig}${dest}${promotion ? (promotion === 'knight' ? 'n' : promotion[0]) : ''}`;
    const move = parseUci(uci);
    if (!move || !this.pos.isLegal(move)) return this.setGround();
    const expected = this.solution[this.moveIndex];
    const after = this.pos.clone();
    after.play(move);
    // like storm: an unexpected checkmate still wins
    if (uci === expected || after.isCheckmate()) {
      this.pos = after;
      this.lastUci = uci;
      this.moveIndex++;
      site.sound.move();
      if (this.moveIndex >= this.solution.length || after.isCheckmate()) this.finishSolve(true);
      else {
        this.setGround();
        setTimeout(this.playOpponentMove, 500);
      }
    } else {
      sound.wrong();
      this.solveFailed = true;
      this.finishSolve(false);
      this.replaySolution();
    }
    this.redraw();
  };

  private playOpponentMove = (): void => {
    const uci = this.solution[this.moveIndex];
    const move = uci && parseUci(uci);
    if (!move) return;
    this.pos.play(move);
    this.lastUci = uci;
    this.moveIndex++;
    site.sound.move();
    this.setGround();
    this.redraw();
  };

  private finishSolve = (win: boolean): void => {
    if (!this.position) return;
    if (win) sound.good();
    this.phase = 'done';
    xhr.solve(this.position.id, win).then(result => {
      this.result = result;
      this.redraw();
    });
  };

  // after a failed attempt, replay the full solution on the board
  private replaySolution = (): void => {
    if (!this.position) return;
    this.replaying = true;
    this.pos = Chess.fromSetup(parseFen(this.position.fen).unwrap()).unwrap();
    this.lastUci = undefined;
    this.moveIndex = 0;
    this.setGround();
    const step = (i: number) => {
      const move = this.solution[i] && parseUci(this.solution[i]);
      if (!move) {
        this.replaying = false;
        this.redraw();
        return;
      }
      this.pos.play(move);
      this.lastUci = this.solution[i];
      site.sound.move();
      this.setGround();
      this.redraw();
      setTimeout(() => step(i + 1), 800);
    };
    setTimeout(() => step(0), 800);
  };

  next = (): void => {
    if (this.loading) return;
    this.loading = true;
    this.redraw();
    xhr
      .next()
      .then(position => {
        this.loading = false;
        this.setPosition(position);
        this.withGround(g => g.set(this.cgConfig()));
        this.redraw();
      })
      .catch(() => {
        this.loading = false;
        this.noMorePositions = true;
        this.redraw();
      });
  };
}
