import { prop } from 'common/common';
import { Clock } from 'puz/clock';
import { Combo } from 'puz/combo';
import CurrentPuzzle from 'puz/current';
import { Run } from 'puz/interfaces';
import { makeSgOpts } from 'puz/run';
import sign from 'puz/sign';
import { getNow, puzzlePov, sound } from 'puz/util';
import { Shogiground } from 'shogiground';
import { Api as SgApi } from 'shogiground/api';
import { MoveOrDrop, Piece, Role, isDrop, isMove } from 'shogiops/types';
import { makeUsi, parseSquareName, parseUsi } from 'shogiops/util';
import { pieceForcePromote } from 'shogiops/variant/util';
import config from './config';
import { StormData, StormOpts, StormPrefs, StormRecap, StormVm } from './interfaces';
import * as xhr from './xhr';

export default class StormCtrl {
  private data: StormData;
  redraw: () => void;
  pref: StormPrefs;
  run: Run;
  vm: StormVm;
  trans: Trans;
  shogiground: SgApi;

  constructor(opts: StormOpts, redraw: (data: StormData) => void) {
    this.data = opts.data;
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.trans = window.lishogi.trans(opts.i18n);
    this.shogiground = Shogiground();
    this.run = {
      pov: puzzlePov(this.data.puzzles[0]),
      moves: 0,
      errors: 0,
      current: new CurrentPuzzle(0, this.data.puzzles[0]),
      clock: new Clock(config),
      history: [],
      combo: new Combo(config),
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      signed: prop(undefined),
      lateStart: false,
      filterFailed: false,
      filterSlow: false,
    };
    this.checkDupTab();
    setTimeout(this.hotkeys, 1000);
    if (this.data.key) setTimeout(() => sign(this.data.key!).then(this.vm.signed), 1000 * 40);
    setTimeout(() => {
      if (!this.run.clock.startAt) {
        this.vm.lateStart = true;
        this.redraw();
      }
    }, config.timeToStart + 1000);
  }

  end = (): void => {
    this.run.history.reverse();
    this.run.endAt = getNow();
    this.redraw();
    sound.end();
    xhr.record(this.runStats(), this.data.notAnExploit).then(res => {
      this.vm.response = res;
      this.redraw();
    });
    this.redrawSlow();
  };

  endNow = (): void => {
    this.pushToHistory(false);
    this.end();
  };

  userMove = (orig: Key, dest: Key, prom: boolean): void => {
    this.playUserMove(orig, dest, prom);
  };

  userDrop = (piece: Piece, dest: Key): void => {
    const move = {
      role: piece.role,
      to: parseSquareName(dest)!,
    };
    this.finishMoveOrDrop(move);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: boolean): void => {
    const move = {
      from: parseSquareName(orig)!,
      to: parseSquareName(dest)!,
      promotion: !!promotion,
    };
    this.finishMoveOrDrop(move);
  };

  private finishMoveOrDrop(md: MoveOrDrop) {
    this.run.clock.start();
    this.run.moves++;

    const puzzle = this.run.current;
    const pos = puzzle.position();
    const usi = makeUsi(md);

    let captureSound = pos.board.occupied.has(md.to);

    pos.play(md);
    if (
      pos.isCheckmate() ||
      usi == puzzle.expectedMove() ||
      (!isDrop(md) &&
        this.isSameMove(usi, puzzle.expectedMove(), puzzle.isAmbPromotion(), pos.turn, pos.board.getRole(md.from)))
    ) {
      puzzle.moveIndex++;
      this.run.combo.inc();
      this.run.modifier.moveAt = getNow();
      const bonus = this.run.combo.bonus();
      if (bonus) {
        this.run.modifier.bonus = bonus;
        this.run.clock.addSeconds(bonus.seconds);
      }
      if (puzzle.isOver()) {
        this.pushToHistory(true);
        if (!this.incPuzzle()) this.end();
      } else {
        puzzle.moveIndex++;
        captureSound = captureSound || pos.board.occupied.has(parseUsi(puzzle.line[puzzle.moveIndex])!.to);
      }
      sound.move(captureSound);
    } else {
      sound.wrong();
      this.pushToHistory(false);
      this.run.errors++;
      this.run.combo.reset();
      this.run.clock.addSeconds(-config.clock.malus);
      this.run.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow(),
      };

      if (this.run.clock.flag()) this.end();
      else if (!this.incPuzzle()) this.end();
    }
    this.redraw();
    this.redrawQuick();
    this.redrawSlow();
    this.shogiground.set(makeSgOpts(this.run, !this.run.endAt));
    window.lishogi.pubsub.emit('ply', this.run.moves);
  }

  // When not promotion isn't an option usi in solution might not contain '+'
  private isSameMove(u1: string, u2: string, ignoreProm: boolean, turn: Color, role?: Role): boolean {
    const usi1 = parseUsi(u1)!,
      usi2 = parseUsi(u2)!;
    if (isDrop(usi1) && isDrop(usi2)) {
      return usi1.role === usi2.role && usi1.to === usi2.to;
    } else if (isMove(usi1) && isMove(usi2)) {
      return (
        usi1.from === usi2.from &&
        usi1.to === usi2.to &&
        (ignoreProm ||
          !!usi1.promotion === !!usi2.promotion ||
          (!!role && pieceForcePromote('standard')({ role: role, color: turn }, usi1.to)))
      );
    }
    return false;
  }

  private redrawQuick = () => setTimeout(this.redraw, 100);
  private redrawSlow = () => setTimeout(this.redraw, 1000);

  private pushToHistory = (win: boolean) =>
    this.run.history.push({
      puzzle: this.run.current.puzzle,
      win,
      millis: this.run.history.length ? getNow() - this.run.current.startAt : 0, // first one is free
    });

  private incPuzzle = (): boolean => {
    const index = this.run.current.index;
    if (index < this.data.puzzles.length - 1) {
      this.run.current = new CurrentPuzzle(index + 1, this.data.puzzles[index + 1]);
      return true;
    }
    return false;
  };

  countWins = (): number => this.run.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);

  runStats = (): StormRecap => ({
    puzzles: this.run.history.length,
    score: this.countWins(),
    moves: this.run.moves,
    errors: this.run.errors,
    combo: this.run.combo.best,
    time: (this.run.endAt! - this.run.clock.startAt!) / 1000,
    highest: this.run.history.reduce((h, r) => (r.win && r.puzzle.rating > h ? r.puzzle.rating : h), 0),
    signed: this.vm.signed(),
  });

  toggleFilterSlow = () => {
    this.vm.filterSlow = !this.vm.filterSlow;
    this.redraw();
  };

  toggleFilterFailed = () => {
    this.vm.filterFailed = !this.vm.filterFailed;
    this.redraw();
  };

  private checkDupTab = () => {
    const dupTabMsg = window.lishogi.storage.make('storm.tab');
    dupTabMsg.fire(this.data.puzzles[0].id);
    dupTabMsg.listen(ev => {
      if (!this.run.clock.startAt && ev.value == this.data.puzzles[0].id) {
        this.vm.dupTab = true;
        this.redraw();
      }
    });
  };

  private hotkeys = () => window.Mousetrap.bind('space', () => location.reload()).bind('return', this.end);
}
