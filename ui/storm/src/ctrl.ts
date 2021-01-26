import * as xhr from './xhr';
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
import { StormOpts, StormData, StormPuzzle, StormVm, Promotion, TimeMod, StormRun } from './interfaces';

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
      puzzleIndex: 0,
      moveIndex: 0,
      clock: config.clock.initial * 1000,
      history: [],
      combo: 0,
      comboBest: 0,
      modifier: {
        moveAt: 0
      },
      run: {
        startAt: 0,
        moves: 0
      }
    };
    // this.vm = { "puzzleIndex": 8, "moveIndex": 0, "clock": 30000, "run": { "startAt": getNow() - 1000 * 30, "endAt": getNow(), moves: 72 }, "history": [{ "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }, { "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }, { "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }, { "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }, { "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }, { "puzzle": { "id": "OZpGP", "fen": "r1b2rk1/p1q2p1p/2p3p1/8/3pP3/3B1Q2/PPPN2Pb/R4R1K b - - 1 16", "line": "h2f4 f3f4 c7f4 f1f4", "rating": 734 }, "win": true, "millis": 0 }, { "puzzle": { "id": "Dzs5L", "fen": "2br4/p5pp/2p1pk2/5r2/3p4/3N1P2/PPPR2PP/2K1R3 w - - 2 22", "line": "d3e5 f5e5 e1e5 f6e5", "rating": 786 }, "win": true, "millis": 2630 }, { "puzzle": { "id": "R05GW", "fen": "4r1k1/6pn/p1Nq1p1p/1p1p2n1/3P4/P3PP2/1B4PP/2Q2RK1 b - - 1 25", "line": "e8c8 c6e7 d6e7 c1c8", "rating": 830 }, "win": true, "millis": 5034 }, { "puzzle": { "id": "uqRFi", "fen": "7r/6b1/3k2p1/p4p2/PpBp1P2/1P4K1/8/4R3 b - - 2 35", "line": "g7f6 e1e6 d6c5 e6f6", "rating": 947 }, "win": true, "millis": 2731 }, { "puzzle": { "id": "CNE6z", "fen": "6k1/q4p1p/2r1p1p1/3pQP2/1Pb3B1/2P5/6PP/4R2K b - - 0 39", "line": "e6f5 e5e8 g8g7 e8c6", "rating": 979 }, "win": false, "millis": 1514 }, { "puzzle": { "id": "vrupJ", "fen": "r1bq1rk1/4ppbp/p2p1np1/1p6/4P3/P1N1B3/1PP1BPPP/R2Q1RK1 b - - 0 12", "line": "f6g4 e2g4 c8g4 d1g4", "rating": 991 }, "win": true, "millis": 4100 }, { "puzzle": { "id": "Y0ZJA", "fen": "3r2k1/r1R3pp/3P1p2/8/1b2P3/6P1/5PKP/3R4 b - - 1 32", "line": "a7c7 d6c7 d8d1 c7c8q", "rating": 1021 }, "win": false, "millis": 8250 }, { "puzzle": { "id": "Vm6wF", "fen": "8/2kP3p/1N4p1/5p2/7b/r4P1P/5P2/4R1K1 w - - 3 42", "line": "e1c1 c7b6", "rating": 1055 }, "win": true, "millis": 2400 }], "combo": 1, "comboBest": 33, "modifier": { "moveAt": 41982, "bonus": { "seconds": 10, "at": 31331 }, "malus": { "seconds": 10, "at": 39582 } }, "puzzleStartAt": 41982 };
    this.promotion = makePromotion(this.withGround, this.makeCgOpts, redraw);
  }

  clockMillis = (): number | undefined =>
    this.vm.run.startAt && Math.max(0, this.vm.run.startAt + this.vm.clock - getNow());

  end = (): void => {
    this.vm.run.endAt = getNow();
    this.ground(false);
    this.redraw();
    this.sound.end();
    xhr.record(this.runStats()).then(res => {
      this.vm.run.response = res;
      this.redraw();
    });
    this.redrawSlow();
  }

  naturalFlag = () => {
    this.pushToHistory(false);
    this.end();
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  }

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    if (!this.vm.run.moves) this.vm.run.startAt = getNow();
    this.vm.run.moves++;
    this.promotion.cancel();
    const expected = this.line()[this.vm.moveIndex + 1];
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = this.position();
    const move = parseUci(uci)!;
    const capture = pos.board.occupied.has(move.to);
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
        this.redrawSlow();
        this.sound.bonus();
      }
      this.redrawQuick();
      lichess.sound.play(capture ? 'capture' : 'move');
      if (this.vm.moveIndex == this.line().length - 1) {
        this.pushToHistory(true);
        this.vm.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
      } else {
        this.vm.moveIndex++;
      }
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.vm.combo = 0;
      this.vm.clock -= config.clock.malus * 1000;
      this.vm.modifier.malus = {
        seconds: config.clock.malus,
        at: getNow()
      };
      if (!this.boundedClockMillis()) this.end();
      else {
        this.vm.moveIndex = 0;
        if (!this.incPuzzle()) this.end();
        this.redrawQuick();
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

  boundedClockMillis = () => this.vm.run.startAt ?
    Math.max(0, this.vm.run.startAt + this.vm.clock - getNow()) :
    this.vm.clock;

  private pushToHistory = (win: boolean) => {
    const now = getNow();
    this.vm.history.push({
      puzzle: this.puzzle(),
      win,
      millis: this.vm.puzzleStartAt ? now - this.vm.puzzleStartAt : 0
    });
    this.vm.puzzleStartAt = now;
  };

  private incPuzzle = (): boolean => {
    if (this.vm.puzzleIndex < this.data.puzzles.length - 1) {
      this.vm.puzzleIndex++;
      return true;
    }
    return false;
  }

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
    const canMove = !this.vm.run.endAt;
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
    const levels = config.combo.levels;
    const lastLevel = levels[levels.length - 1];
    if (lvl >= levels.length - 1) {
      const range = (lastLevel[0] - levels[levels.length - 2][0]);
      return ((this.vm.combo - lastLevel[0]) / range) * 100 % 100;
    }
    const bounds = [levels[lvl][0], levels[lvl + 1][0]];
    return Math.floor((this.vm.combo - bounds[0]) / (bounds[1] - bounds[0]) * 100);
  };

  countWins = (): number => this.vm.history.reduce((c, r) => c + (r.win ? 1 : 0), 0);

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  }

  runStats = (): StormRun => ({
    puzzles: this.vm.history.length,
    score: this.countWins(),
    moves: this.vm.run.moves,
    combo: this.vm.comboBest,
    time: (this.vm.run.endAt! - this.vm.run.startAt) / 1000,
    highest: this.vm.history.reduce((h, r) => r.win && r.puzzle.rating > h ? r.puzzle.rating : h, 0)
  });

  private showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  private uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];

  private loadSound = (file: string, volume?: number) => {
    lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`);
    return () => lichess.sound.play(file, volume);
  };

  private sound = {
    bonus: this.loadSound('other/ping'),
    end: this.loadSound('other/gewonnen', 0.6)
  };
}
