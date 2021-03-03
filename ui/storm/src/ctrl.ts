import * as xhr from './xhr';
import config from './config';
import makePromotion from 'puz/promotion';
import sign from './sign';
import { Api as CgApi } from 'chessground/api';
import { Chess } from 'chessops/chess';
import { chessgroundDests } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { getNow } from 'puz/util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci, opposite } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { StormOpts, StormData, StormVm, StormRecap, StormPrefs } from './interfaces';
import { Promotion, Puzzle, Run, TimeMod } from 'puz/interfaces';

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
      puzzleIndex: 0,
      moveIndex: 0,
      clockMs: config.clock.initial * 1000,
      history: [],
      combo: {
        current: 0,
        best: 0,
      },
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      signed: prop(undefined),
      lateStart: false,
      filterFailed: false,
    };
    this.promotion = makePromotion(this.withGround, this.makeCgOpts, this.redraw);
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
    if (!this.run.puzzleStartAt) return;
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

  naturalFlag = (): void => {
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
    const line = this.line();
    const expected = line[this.run.moveIndex + 1];
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = this.position();
    const move = parseUci(uci)!;
    let captureSound = pos.board.occupied.has(move.to);
    pos.play(move);
    if (pos.isCheckmate() || uci == expected) {
      this.run.moveIndex++;
      this.run.combo.current++;
      this.run.combo.best = Math.max(this.run.combo.best, this.run.combo.current);
      this.run.modifier.moveAt = getNow();
      const bonus = this.computeComboBonus();
      if (bonus) {
        this.run.modifier.bonus = bonus;
        this.run.clockMs += bonus.seconds * 1000;
        this.sound.bonus();
      }
      if (this.run.moveIndex == line.length - 1) {
        this.pushToHistory(true);
        this.run.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
      } else {
        this.run.moveIndex++;
        captureSound = captureSound || pos.board.occupied.has(parseUci(line[this.run.moveIndex]!)!.to);
      }
      this.sound.move(captureSound);
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.run.combo.current = 0;
      this.run.clockMs -= config.clock.malus * 1000;
      this.run.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow(),
      };
      if (!this.boundedClockMillis()) this.end();
      else {
        this.run.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
      }
    }
    this.redraw();
    this.redrawQuick();
    this.redrawSlow();
    this.withGround(this.showGround);
    lichess.pubsub.emit('ply', this.run.moves);
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
    this.run.startAt ? Math.max(0, this.run.startAt + this.run.clockMs - getNow()) : this.run.clockMs;

  private pushToHistory = (win: boolean) => {
    const now = getNow();
    this.run.history.push({
      puzzle: this.puzzle(),
      win,
      millis: this.run.puzzleStartAt ? now - this.run.puzzleStartAt : 0,
    });
    this.run.puzzleStartAt = now;
  };

  private incPuzzle = (): boolean => {
    if (this.run.puzzleIndex < this.data.puzzles.length - 1) {
      this.run.puzzleIndex++;
      return true;
    }
    return false;
  };

  puzzle = (): Puzzle => this.data.puzzles[this.run.puzzleIndex];

  line = (): Uci[] => this.puzzle().line.split(' ');

  position = (): Chess => {
    const pos = Chess.fromSetup(parseFen(this.puzzle().fen).unwrap()).unwrap();
    this.line()
      .slice(0, this.run.moveIndex + 1)
      .forEach(uci => pos.play(parseUci(uci)!));
    return pos;
  };

  makeCgOpts = (): CgConfig => {
    const puzzle = this.puzzle();
    const pos = this.position();
    const pov = opposite(parseFen(puzzle.fen).unwrap().turn);
    const canMove = !this.run.endAt;
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
      check: !!pos.isCheck(),
      lastMove: this.uciToLastMove(this.line()[this.run.moveIndex]),
    };
  };

  comboLevel = () =>
    config.combo.levels.reduce((lvl, [threshold, _], index) => (threshold <= this.run.combo.current ? index : lvl), 0);

  comboPercent = () => {
    const lvl = this.comboLevel();
    const levels = config.combo.levels;
    const lastLevel = levels[levels.length - 1];
    if (lvl >= levels.length - 1) {
      const range = lastLevel[0] - levels[levels.length - 2][0];
      return (((this.run.combo.current - lastLevel[0]) / range) * 100) % 100;
    }
    const bounds = [levels[lvl][0], levels[lvl + 1][0]];
    return Math.floor(((this.run.combo.current - bounds[0]) / (bounds[1] - bounds[0])) * 100);
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
    errors: this.run.history.filter(r => !r.win).length,
    combo: this.run.combo.best,
    time: (this.run.endAt! - this.run.startAt!) / 1000,
    highest: this.run.history.reduce((h, r) => (r.win && r.puzzle.rating > h ? r.puzzle.rating : h), 0),
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
      if (!this.run.startAt && ev.value == this.data.puzzles[0].id) {
        this.vm.dupTab = true;
        this.redraw();
      }
    });
  };

  private hotkeys = () => window.Mousetrap.bind('space', () => location.reload()).bind('return', this.end);
}
