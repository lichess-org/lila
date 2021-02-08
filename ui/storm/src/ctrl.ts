import * as xhr from './xhr';
import config from './config';
import makePromotion from './promotion';
import sign from './sign';
import { Api as CgApi } from 'chessground/api';
import { Chess } from 'chessops/chess';
import { chessgroundDests } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { getNow } from './util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci, opposite } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { StormOpts, StormData, StormPuzzle, StormVm, Promotion, TimeMod, StormRun, StormPrefs } from './interfaces';

export default class StormCtrl {
  private data: StormData;
  private redraw: () => void;
  pref: StormPrefs;
  vm: StormVm;
  trans: Trans;
  promotion: Promotion;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;

  constructor(opts: StormOpts, redraw: (data: StormData) => void) {
    this.data = opts.data;
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.trans = lichess.trans(opts.i18n);
    this.vm = {
      puzzleIndex: 0,
      moveIndex: 0,
      clock: config.clock.initial * 1000,
      history: [],
      combo: 0,
      comboBest: 0,
      modifier: {
        moveAt: 0,
      },
      run: {
        startAt: 0,
        moves: 0,
        errors: 0,
      },
      signed: prop(undefined),
      lateStart: false,
      filterFailed: false,
    };
    this.promotion = makePromotion(this.withGround, this.makeCgOpts, this.redraw);
    this.checkDupTab();
    setTimeout(this.hotkeys, 1000);
    if (this.data.key) setTimeout(() => sign(this.data.key!).then(this.vm.signed), 1000 * 40);
    setTimeout(() => {
      if (!this.vm.run.startAt) {
        this.vm.lateStart = true;
        this.redraw();
      }
    }, config.timeToStart + 1000);
  }

  clockMillis = (): number | undefined =>
    this.vm.run.startAt && Math.max(0, this.vm.run.startAt + this.vm.clock - getNow());

  end = (): void => {
    if (!this.vm.puzzleStartAt) return;
    this.vm.history.reverse();
    this.vm.run.endAt = getNow();
    this.ground(false);
    this.redraw();
    this.sound.end();
    xhr.record(this.runStats(), this.data.notAnExploit).then(res => {
      this.vm.run.response = res;
      this.redraw();
    });
    this.redrawSlow();
  };

  endNow = (): void => {
    this.pushToHistory(false);
    this.end();
  };

  naturalFlag = (): void => {
    this.pushToHistory(false);
    this.end();
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    if (!this.vm.run.moves) this.vm.run.startAt = getNow();
    this.vm.run.moves++;
    this.promotion.cancel();
    const line = this.line();
    const expected = line[this.vm.moveIndex + 1];
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = this.position();
    const move = parseUci(uci)!;
    let captureSound = pos.board.occupied.has(move.to);
    pos.play(move);
    if (pos.isCheckmate() || uci == expected) {
      this.vm.moveIndex++;
      this.vm.combo++;
      this.vm.comboBest = Math.max(this.vm.comboBest, this.vm.combo);
      this.vm.modifier.moveAt = getNow();
      const bonus = this.computeComboBonus();
      if (bonus) {
        this.vm.modifier.bonus = bonus;
        this.vm.clock += bonus.seconds * 1000;
        this.sound.bonus();
      }
      if (this.vm.moveIndex == line.length - 1) {
        this.pushToHistory(true);
        this.vm.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
      } else {
        this.vm.moveIndex++;
        captureSound = captureSound || pos.board.occupied.has(parseUci(line[this.vm.moveIndex]!)!.to);
      }
      this.sound.move(captureSound);
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.vm.run.errors++;
      this.vm.combo = 0;
      this.vm.clock -= config.clock.malus * 1000;
      this.vm.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow(),
      };
      if (!this.boundedClockMillis()) this.end();
      else {
        this.vm.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
      }
    }
    this.redraw();
    this.redrawQuick();
    this.redrawSlow();
    this.withGround(this.showGround);
    lichess.pubsub.emit('ply', this.vm.run.moves);
  };

  private redrawQuick = () => setTimeout(this.redraw, 100);
  private redrawSlow = () => setTimeout(this.redraw, 1000);

  private computeComboBonus = (): TimeMod | undefined => {
    if (this.comboPercent() == 0) {
      const level = this.comboLevel();
      if (level > 0)
        return {
          seconds: config.combo.levels[level][1],
          at: getNow(),
        };
    }
    return;
  };

  boundedClockMillis = () =>
    this.vm.run.startAt ? Math.max(0, this.vm.run.startAt + this.vm.clock - getNow()) : this.vm.clock;

  private pushToHistory = (win: boolean) => {
    const now = getNow();
    this.vm.history.push({
      puzzle: this.puzzle(),
      win,
      millis: this.vm.puzzleStartAt ? now - this.vm.puzzleStartAt : 0,
    });
    this.vm.puzzleStartAt = now;
  };

  private incPuzzle = (): boolean => {
    if (this.vm.puzzleIndex < this.data.puzzles.length - 1) {
      this.vm.puzzleIndex++;
      return true;
    }
    return false;
  };

  puzzle = (): StormPuzzle => this.data.puzzles[this.vm.puzzleIndex];

  line = (): Uci[] => this.puzzle().line.split(' ');

  position = (): Chess => {
    const pos = Chess.fromSetup(parseFen(this.puzzle().fen).unwrap()).unwrap();
    this.line()
      .slice(0, this.vm.moveIndex + 1)
      .forEach(uci => pos.play(parseUci(uci)!));
    return pos;
  };

  makeCgOpts = (): CgConfig => {
    const puzzle = this.puzzle();
    const pos = this.position();
    const pov = opposite(parseFen(puzzle.fen).unwrap().turn);
    const canMove = !this.vm.run.endAt;
    return {
      fen: makeFen(pos.toSetup()),
      orientation: pov,
      turnColor: pos.turn,
      movable: canMove
        ? {
            color: pov,
            dests: chessgroundDests(pos),
          }
        : undefined,
      premovable: {
        enabled: false,
      },
      check: !!pos.isCheck(),
      lastMove: this.uciToLastMove(this.line()[this.vm.moveIndex]),
    };
  };

  comboLevel = () =>
    config.combo.levels.reduce((lvl, [threshold, _], index) => (threshold <= this.vm.combo ? index : lvl), 0);

  comboPercent = () => {
    const lvl = this.comboLevel();
    const levels = config.combo.levels;
    const lastLevel = levels[levels.length - 1];
    if (lvl >= levels.length - 1) {
      const range = lastLevel[0] - levels[levels.length - 2][0];
      return (((this.vm.combo - lastLevel[0]) / range) * 100) % 100;
    }
    const bounds = [levels[lvl][0], levels[lvl + 1][0]];
    return Math.floor(((this.vm.combo - bounds[0]) / (bounds[1] - bounds[0])) * 100);
  };

  countWins = (): number => this.vm.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  runStats = (): StormRun => ({
    puzzles: this.vm.history.length,
    score: this.countWins(),
    moves: this.vm.run.moves,
    errors: this.vm.run.errors,
    combo: this.vm.comboBest,
    time: (this.vm.run.endAt! - this.vm.run.startAt) / 1000,
    highest: this.vm.history.reduce((h, r) => (r.win && r.puzzle.rating > h ? r.puzzle.rating : h), 0),
    signed: this.vm.signed(),
  });

  toggleFilterFailed = () => {
    this.vm.filterFailed = !this.vm.filterFailed;
    this.redraw();
  };

  private showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  private uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];

  private loadSound = (file: string, volume?: number, delay?: number) => {
    setTimeout(() => lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`), delay || 1000);
    return () => lichess.sound.play(file, volume);
  };

  private sound = {
    move: (take: boolean) => lichess.sound.play(take ? 'capture' : 'move'),
    bonus: this.loadSound('other/ping', 0.8, 1000),
    end: this.loadSound('other/gewonnen', 0.6, 5000),
  };

  private checkDupTab = () => {
    const dupTabMsg = lichess.storage.make('storm.tab');
    dupTabMsg.fire(this.data.puzzles[0].id);
    dupTabMsg.listen(ev => {
      if (!this.vm.run.startAt && ev.value == this.data.puzzles[0].id) {
        this.vm.dupTab = true;
        this.redraw();
      }
    });
  };

  private hotkeys = () => {
    window.Mousetrap.bind('space', () => location.reload()).bind('return', this.end);
  };
}
