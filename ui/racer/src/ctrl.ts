import config from 'puz/config';
import makePromotion from 'puz/promotion';
import sign from 'puz/sign';
import { Api as CgApi } from 'chessground/api';
import { getNow, loadSound } from 'puz/util';
import { makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { RacerOpts, RacerData, RacerVm, RacerPrefs } from './interfaces';
import { Promotion, Run } from 'puz/interfaces';
import { Combo } from 'puz/combo';
import CurrentPuzzle from 'puz/current';
import { Clock } from 'puz/clock';

export default class StormCtrl {
  private data: RacerData;
  private redraw: () => void;
  pref: RacerPrefs;
  run: Run;
  vm: RacerVm;
  trans: Trans;
  promotion: Promotion;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;

  constructor(opts: RacerOpts, redraw: (data: RacerData) => void) {
    this.data = opts.data;
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.trans = lichess.trans(opts.i18n);
    this.run = {
      moves: 0,
      errors: 0,
      current: new CurrentPuzzle(0, this.data.puzzles[0]),
      clock: new Clock(),
      history: [],
      combo: new Combo(),
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      signed: prop(undefined),
    };
    this.promotion = makePromotion(this.withGround, () => makeCgOpts(this.run), this.redraw);
    if (this.data.key) setTimeout(() => sign(this.data.key!).then(this.vm.signed), 1000 * 40);
  }

  end = (): void => {
    this.run.history.reverse();
    this.run.endAt = getNow();
    this.ground(false);
    this.redraw();
    this.sound.end();
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
        this.run.clock.addSeconds(bonus.seconds);
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

  private sound = {
    move: (take: boolean) => lichess.sound.play(take ? 'capture' : 'move'),
    bonus: loadSound('other/ping', 0.8, 1000),
    end: loadSound('other/gewonnen', 0.6, 5000),
  };
}
