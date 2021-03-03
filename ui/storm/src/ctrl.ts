import * as xhr from './xhr';
import config from 'puz/config';
import makePromotion from 'puz/promotion';
import sign from './sign';
import { Api as CgApi } from 'chessground/api';
import { getNow, loadSound } from 'puz/util';
import { boundedClockMillis, makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { StormOpts, StormData, StormVm, StormRecap, StormPrefs } from './interfaces';
import { Promotion, Run } from 'puz/interfaces';
import { Combo } from 'puz/combo';
import CurrentPuzzle from 'puz/current';

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
      moves: 0,
      errors: 0,
      current: new CurrentPuzzle(0, this.data.puzzles[0]),
      clockMs: config.clock.initial * 1000,
      history: [],
      combo: new Combo(),
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      signed: prop(undefined),
      lateStart: false,
      filterFailed: false,
    };
    this.promotion = makePromotion(this.withGround, () => makeCgOpts(this.run), this.redraw);
    this.checkDupTab();
    setTimeout(this.hotkeys, 1000);
    if (this.data.key) setTimeout(() => sign(this.data.key!).then(this.vm.signed), 1000 * 40);
    setTimeout(() => {
      if (!this.run.startAt) {
        this.vm.lateStart = true;
        this.redraw();
      }
    }, config.timeToStart + 1000);
  }

  clockMillis = (): number | undefined =>
    this.run.startAt && Math.max(0, this.run.startAt + this.run.clockMs - getNow());

  end = (): void => {
    this.run.history.reverse();
    this.run.endAt = getNow();
    this.ground(false);
    this.redraw();
    this.sound.end();
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
    if (!this.run.moves) this.run.startAt = getNow();
    this.run.moves++;
    this.promotion.cancel();
    const cur = this.run.current;
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = cur.position();
    const move = parseUci(uci)!;
    let captureSound = pos.board.occupied.has(move.to);
    pos.play(move);
    if (pos.isCheckmate() || uci == cur.expectedMove()) {
      cur.moveIndex++;
      this.run.combo.inc();
      this.run.modifier.moveAt = getNow();
      const bonus = this.run.combo.bonus();
      if (bonus) {
        this.run.modifier.bonus = bonus;
        this.run.clockMs += bonus.seconds * 1000;
        this.sound.bonus();
      }
      if (cur.isOver()) {
        this.pushToHistory(true);
        if (!this.incPuzzle()) this.end();
      } else {
        cur.moveIndex++;
        captureSound = captureSound || pos.board.occupied.has(parseUci(cur.line[cur.moveIndex]!)!.to);
      }
      this.sound.move(captureSound);
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.run.errors++;
      this.run.combo.reset();
      this.run.clockMs -= config.clock.malus * 1000;
      this.run.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow(),
      };
      if (!boundedClockMillis(this.run)) this.end();
      else if (!this.incPuzzle()) this.end();
    }
    this.redraw();
    this.redrawQuick();
    this.redrawSlow();
    this.withGround(g => g.set(makeCgOpts(this.run)));
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
      return true;
    }
    return false;
  };

  countWins = (): number => this.run.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  runStats = (): StormRecap => ({
    puzzles: this.run.history.length,
    score: this.countWins(),
    moves: this.run.moves,
    errors: this.run.errors,
    combo: this.run.combo.best,
    time: (this.run.endAt! - this.run.startAt!) / 1000,
    highest: this.run.history.reduce((h, r) => (r.win && r.puzzle.rating > h ? r.puzzle.rating : h), 0),
    signed: this.vm.signed(),
  });

  toggleFilterFailed = () => {
    this.vm.filterFailed = !this.vm.filterFailed;
    this.redraw();
  };

  private sound = {
    move: (take: boolean) => lichess.sound.play(take ? 'capture' : 'move'),
    bonus: loadSound('other/ping', 0.8, 1000),
    end: loadSound('other/gewonnen', 0.6, 5000),
  };

  private checkDupTab = () => {
    const dupTabMsg = lichess.storage.make('storm.tab');
    dupTabMsg.fire(this.data.puzzles[0].id);
    dupTabMsg.listen(ev => {
      if (!this.run.startAt && ev.value == this.data.puzzles[0].id) {
        this.vm.dupTab = true;
        this.redraw();
      }
    });
  };

  private hotkeys = () => window.Mousetrap.bind('space', () => location.reload()).bind('return', this.end);
}
