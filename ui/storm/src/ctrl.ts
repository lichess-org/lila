import config from './config';
import makePromotion from './promotion';
import { Api as CgApi } from 'chessground/api';
import { Chess } from 'chessops/chess';
import { chessgroundDests } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { getNow } from './util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci, opposite } from 'chessops/util';
import { prop, Prop } from 'common';
import { Role } from 'chessground/types';
import { StormOpts, StormData, StormPuzzle, StormVm, Promotion, TimeMod } from './interfaces';

export default class StormCtrl {

  data: StormData;
  vm: StormVm;
  trans: Trans;
  promotion: Promotion;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;

  constructor(readonly opts: StormOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = lichess.trans(opts.i18n);
    this.vm = {
      mode: 'play',
      puzzleIndex: 0,
      moveIndex: 0,
      clock: {
        budget: config.clock.initial * 1000,
      },
      history: [],
      combo: 0,
      modifier: {
        moveAt: 0
      }
    };
    this.promotion = makePromotion(this.withGround, this.makeCgOpts, redraw);
  }

  clockMillis = (): number | undefined =>
    this.vm.clock.startAt && Math.max(0, this.vm.clock.startAt + this.vm.clock.budget - getNow());

  end = (): void => {
    this.vm.mode = 'end';
    this.vm.clock.startAt = undefined;
    this.ground(false);
    this.redraw();
  }

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  }

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    if (!this.vm.clock.startAt) this.vm.clock.startAt = getNow();
    this.promotion.cancel();
    const expected = this.line()[this.vm.moveIndex + 1];
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = this.position();
    pos.play(parseUci(uci)!);
    if (pos.isCheckmate() || uci == expected) {
      this.vm.moveIndex++;
      this.vm.combo++;
      this.vm.modifier.moveAt = getNow();
      const bonus = this.computeComboBonus();
      if (bonus) {
        this.vm.modifier.bonus = bonus;
        this.vm.clock.budget += bonus.seconds * 1000;
        this.redrawSlow();
      }
      this.redrawQuick();
      lichess.sound.play('move');
      if (this.vm.moveIndex == this.line().length - 1) {
        this.pushToHistory(true);
        this.vm.puzzleIndex++;
        this.vm.moveIndex = 0;
      } else {
        this.vm.moveIndex++;
      }
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.vm.combo = 0;
      this.vm.clock.budget -= config.clock.malus * 1000;
      this.vm.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow()
      };
      if (!this.boundedClockMillis()) this.end();
      else {
        this.vm.puzzleIndex++;
        this.vm.moveIndex = 0;
        this.redrawSlow();
      }
    }
    this.redraw();
    this.withGround(this.showGround);
  };

  private redrawQuick = () => setTimeout(this.redraw, 100);
  private redrawSlow = () => setTimeout(this.redraw, 1000);

  private computeComboBonus = (): TimeMod | undefined => {
    if (this.comboPercent() == 0) {
      const level = this.comboLevel();
      if (level > 0) return {
        seconds: config.combo.levels[level][1],
        at: getNow()
      };
    }
    return;
  };

  boundedClockMillis = () => this.vm.clock.startAt ?
    Math.max(0, this.vm.clock.startAt + this.vm.clock.budget - getNow()) :
    this.vm.clock.budget;

  private pushToHistory = (win: boolean) => {
    const now = getNow();
    this.vm.history.push({
      puzzle: this.puzzle(),
      win,
      millis: this.vm.puzzleStartAt ? now - this.vm.puzzleStartAt : 0
    });
    this.vm.puzzleStartAt = now;
  };

  puzzle = (): StormPuzzle => this.data.puzzles[this.vm.puzzleIndex];

  line = (): Uci[] => this.puzzle().line.split(' ');

  position = (): Chess => {
    const pos = Chess.fromSetup(parseFen(this.puzzle().fen).unwrap()).unwrap();
    this.line().slice(0, this.vm.moveIndex + 1).forEach(uci =>
      pos.play(parseUci(uci)!)
    );
    return pos;
  }

  makeCgOpts = (): CgConfig => {
    const puzzle = this.puzzle();
    const pos = this.position();
    const pov = opposite(parseFen(puzzle.fen).unwrap().turn);
    const canMove = this.vm.mode == 'play';
    return {
      fen: makeFen(pos.toSetup()),
      orientation: pov,
      turnColor: pos.turn,
      movable: canMove ? {
        color: pov,
        dests: chessgroundDests(pos)
      } : undefined,
      premovable: {
        enabled: false
      },
      check: !!pos.isCheck(),
      lastMove: this.uciToLastMove(this.line()[this.vm.moveIndex])
    };
  }

  comboLevel = () => config.combo.levels.reduce((lvl, [threshold, _], index) => threshold <= this.vm.combo ? index : lvl, 0);

  comboPercent = () => {
    const lvl = this.comboLevel();
    if (lvl == config.combo.levels.length - 1) return 100;
    const bounds = [config.combo.levels[lvl][0], config.combo.levels[lvl + 1][0]];
    return Math.floor((this.vm.combo - bounds[0]) / (bounds[1] - bounds[0]) * 100);
  };

  countWins = (): number => this.vm.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  }

  private showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  private uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];
}
