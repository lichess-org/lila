import * as xhr from './xhr';
import config from './config';
import makePromotion from 'puz/promotion';
import sign from 'puz/sign';
import { Api as CgApi } from 'chessground/api';
import { getNow, puzzlePov, sound } from 'puz/util';
import { makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { StormOpts, StormData, StormVm, StormRecap, StormPrefs } from './interfaces';
import { Promotion, Run } from 'puz/interfaces';
import { Combo } from 'puz/combo';
import CurrentPuzzle from 'puz/current';
import { Clock } from 'puz/clock';

export default class StormCtrl {
  private data: StormData;
  private redraw: () => void;
  pref: StormPrefs;
  run: Run;
  vm: StormVm;
  trans: Trans;
  promotion: Promotion;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;

  constructor(opts: StormOpts, redraw: (data: StormData) => void) {
    this.data = opts.data;
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.trans = lichess.trans(opts.i18n);
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
    this.promotion = makePromotion(this.withGround, () => makeCgOpts(this.run, !this.run.endAt), this.redraw);
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
    this.ground(false);
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

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    this.run.clock.start();
    this.run.moves++;
    this.promotion.cancel();
    const puzzle = this.run.current;
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = puzzle.position();
    const move = parseUci(uci)!;
    let captureSound = pos.board.occupied.has(move.to);
    pos.play(move);
    const correct = pos.isCheckmate() || uci == puzzle.expectedMove();
    if (!puzzle.moveIndex) mon.firstMove(correct);
    if (correct) {
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
        captureSound = captureSound || pos.board.occupied.has(parseUci(puzzle.line[puzzle.moveIndex]!)!.to);
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
    this.withGround(g => g.set(makeCgOpts(this.run, !this.run.endAt)));
    lichess.pubsub.emit('ply', this.run.moves);
  };

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
      mon.newPuzzle();
      return true;
    }
    return false;
  };

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
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
    const dupTabMsg = lichess.storage.make('storm.tab');
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
