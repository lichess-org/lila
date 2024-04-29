import * as xhr from './xhr';
import config from './config';
import CurrentPuzzle from 'puz/current';
import sign from 'puz/sign';
import { Api as CgApi } from 'chessground/api';
import { Clock } from 'puz/clock';
import { Combo } from 'puz/combo';
import { getNow, puzzlePov, sound } from 'puz/util';
import { makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { PromotionCtrl } from 'chess/promotion';
import { prop } from 'common';
import { PuzCtrl, Run } from 'puz/interfaces';
import { PuzFilters } from 'puz/filters';
import { Role } from 'chessground/types';
import { StormOpts, StormVm, StormRecap, StormPrefs, StormData } from './interfaces';

export default class StormCtrl implements PuzCtrl {
  private data: StormData;
  private redraw: () => void;
  pref: StormPrefs;
  run: Run;
  vm: StormVm;
  filters: PuzFilters;
  trans: Trans;
  promotion: PromotionCtrl;
  ground = prop<CgApi | false>(false);
  flipped = false;

  constructor(opts: StormOpts, redraw: (data: StormData) => void) {
    this.data = { puzzles: opts.puzzles, key: opts.key };
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.filters = new PuzFilters(this.redraw, false);
    this.trans = site.trans(opts.i18n);
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
    };
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.redraw);
    setTimeout(() => {
      this.run.current.moveIndex = 0;
      this.setGround();
    }, 100);
    this.checkDupTab();
    setTimeout(this.hotkeys, 1000);
    if (this.data.key) setTimeout(() => sign(this.data.key!).then(this.vm.signed), 1000 * 40);
    setTimeout(() => {
      if (!this.run.clock.startAt) {
        this.vm.lateStart = true;
        this.redraw();
      }
    }, config.timeToStart + 1000);
    site.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      if (!$('body').hasClass('zen-auto')) {
        xhr.setZen(zen);
      }
    });
    $('#zentog').on('click', this.toggleZen);
    this.run.current.playSound();
  }

  end = (): void => {
    if (this.run.endAt) return;
    this.run.history.reverse();
    this.run.endAt = getNow();
    this.ground(false);
    this.redraw();
    sound.end();
    xhr.record(this.runStats()).then(res => {
      this.vm.response = res;
      this.redraw();
    });
    $('body').toggleClass('playing'); // end zen
    this.redrawSlow();
  };

  endNow = (): void => {
    this.pushToHistory(false);
    this.end();
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, { submit: this.playUserMove })) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): any => {
    const now = getNow();
    const puzzle = this.run.current;
    if (puzzle.startAt + config.minFirstMoveTime > now) console.log('reverted!');
    else {
      this.run.clock.start();
      this.run.moves++;
      this.promotion.cancel();
      const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
      const pos = puzzle.position();
      pos.play(parseUci(uci)!);
      const correct = pos.isCheckmate() || uci == puzzle.expectedMove();
      if (correct) {
        puzzle.moveIndex++;
        this.run.combo.inc();
        this.run.modifier.moveAt = now;
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
        }
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
      this.run.current.playSound(puzzle);
      this.redraw();
      this.redrawQuick();
      this.redrawSlow();
    }
    this.setGround();
    if (this.run.current.moveIndex < 0) {
      this.run.current.moveIndex = 0;
      this.setGround();
    }
    site.pubsub.emit('ply', this.run.moves);
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
      return true;
    }
    return false;
  };

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  private setGround = () => this.withGround(g => g.set(makeCgOpts(this.run, !this.run.endAt, this.flipped)));

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

  flip = () => {
    this.flipped = !this.flipped;
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  private checkDupTab = () => {
    const dupTabMsg = site.storage.make('storm.tab');
    dupTabMsg.fire(this.data.puzzles[0].id);
    dupTabMsg.listen(ev => {
      if (!this.run.clock.startAt && ev.value == this.data.puzzles[0].id) {
        this.vm.dupTab = true;
        this.redraw();
      }
    });
  };

  private toggleZen = () => site.pubsub.emit('zen');

  private hotkeys = () =>
    site.mousetrap
      .bind('space', () => location.reload())
      .bind('return', this.end)
      .bind('f', this.flip)
      .bind('z', this.toggleZen);
}
