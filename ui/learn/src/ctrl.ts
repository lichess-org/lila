import { Api } from 'shogiground/api';
import { Shogiground } from 'shogiground/shogiground';
import { Piece } from 'shogiground/types';
import { opposite } from 'shogiground/util';
import { shogigroundDropDests, shogigroundMoveDests } from 'shogiops/compat';
import { Role } from 'shogiops/types';
import { makeUsi, parseSquareName, parseUsi } from 'shogiops/util';
import * as categories from './categories';
import * as ground from './ground';
import { Category, LearnOpts, Redraw, Stage, Vm } from './interfaces';
import { calcScore } from './level';
import { ProgressStorage } from './progress';
import {
  findCapture,
  findRandomMove,
  findUnprotectedCapture,
  illegalShogigroundDropDests,
  illegalShogigroundMoveDests,
  inCheck,
} from './shogi';
import { currentPosition, sound } from './util';

const li = window.lishogi;

export default class LearnCtrl {
  shogiground: Api;

  categories: Category[];
  stages: Stage[];

  vm?: Vm; // active lesson, if active

  timeouts: number[] = [];
  progress: ProgressStorage;
  trans: Trans;
  pref: any;

  constructor(
    private opts: LearnOpts,
    readonly redraw: Redraw
  ) {
    this.trans = li.trans(this.opts.i18n);
    this.pref = opts.pref;

    this.categories = categories.categories;
    this.stages = categories.stages;

    this.progress = new ProgressStorage(this.opts);

    this.shogiground = Shogiground(ground.initConfig(this));

    const hash = window.location.hash.split('/').filter(c => c !== '#' && c !== '');
    if (hash.length > 0) {
      this.setLesson(parseInt(hash[0]), parseInt(hash[1]));
    }
  }

  setHome(): void {
    this.clearTimetouts();
    this.vm = undefined;
    window.history.replaceState('', '', '/learn');
  }

  setLesson(stageId: number, levelId: number = 1): void {
    const category = this.categories.find(c => c.stages.some(s => s.id === stageId)),
      stage = category && category.stages.find(s => s.id === stageId),
      level = stage && stage.levels.find(l => l.id === levelId);

    if (level) {
      this.clearTimetouts();
      const prevStage = this.vm?.stage;
      this.vm = {
        category,
        sideCategory: category.key,
        stage,
        stageState: prevStage?.id !== stage.id && !this.progress.has(stage.key) && level.id === 1 ? 'init' : 'running',
        level,
        levelState: 'play',
        usiCList: [],
      };
      this.shogiground.selectSquare(null);
      this.shogiground.set(ground.initLevelConfig(level), true);
      this.applyOpponentMoveOrDrop();
      this.shogiground.set(ground.destsAndCheck(level, this.vm.usiCList));

      window.history.replaceState('', '', '/learn#/' + stage.id + '/' + level.id);
      window.scrollTo(0, 0);
      if (this.vm.stageState === 'init') sound.start();
    } else this.setHome();
  }

  nextLesson(): void {
    this.clearTimetouts();
    if (this.vm) {
      if (this.vm.level.id < this.vm.stage.levels.length) this.setLesson(this.vm.stage.id, this.vm.level.id + 1);
      else if (this.vm.stage.id < categories.stages.length) this.setLesson(this.vm.stage.id + 1);
      else this.setHome();
    }
  }

  restartLevel(): void {
    if (this.vm) {
      this.setLesson(this.vm.stage.id, this.vm.level.id);
    }
    this.redraw();
  }

  completeLevel(): void {
    if (this.vm) {
      if (this.vm.level.id === this.vm.stage.levels.length) {
        this.vm.stageState = 'completed';
        sound.end();
      }
      this.vm.levelState = 'completed';
      this.vm.score = calcScore(this.vm.level, this.vm.usiCList);
      this.progress.saveScore(this.vm.stage.key, this.vm.level.id, this.vm.score);
      if (this.vm.stageState !== 'completed' && !this.vm.level.nextButton) {
        this.clearTimetouts();
        this.timeouts.push(
          setTimeout(() => {
            this.nextLesson();
            this.redraw();
          }, 1000)
        );
      }
      this.redraw();
    }
  }

  failLevel(): void {
    if (this.vm) {
      this.vm.levelState = 'fail';
      this.redraw();
    }
  }

  reset(): void {
    this.vm = undefined;
    this.progress.reset();
    this.redraw();
  }

  private clearTimetouts(): void {
    this.timeouts.forEach(t => clearTimeout(t));
    this.timeouts.length = 0;
  }

  private applyOpponentMoveOrDrop(): void {
    if (this.vm && this.vm.level.scenario) {
      const scenario = this.vm.level.scenario,
        usiCList = this.vm.usiCList,
        sUsiC = scenario[usiCList.length];

      if (
        sUsiC &&
        sUsiC.color !== this.vm.level.color &&
        usiCList.every((usiC, i) => {
          return scenario[i].usi === usiC.usi && scenario[i].color === usiC.color;
        })
      ) {
        this.vm.usiCList.push(sUsiC);
        ground.playUsi(this, sUsiC);
      }
    }
  }

  private applyUserMoveOrDrop(usi: Usi): void {
    if (this.vm) {
      this.vm.usiCList.push({ usi, color: this.vm.level.color });
      this.shogiground.set({ checks: inCheck(currentPosition(this.vm.level, this.vm.usiCList)) });

      // make opponents move if available
      this.applyOpponentMoveOrDrop();

      // check if we are done - success or fail
      if (this.vm.level.success(this.vm.level, this.vm.usiCList)) this.completeLevel();
      else if (this.vm.level.failure && this.vm.level.failure(this.vm.level, this.vm.usiCList)) this.failLevel();

      // update shogiground accordingly
      const pos = currentPosition(this.vm.level, this.vm.usiCList),
        hasObstacles = !!this.vm.level.obstacles?.length,
        illegalDests = !!this.vm.level.offerIllegalDests || hasObstacles,
        active = this.vm.levelState !== 'fail' && this.vm.levelState !== 'completed';

      if (this.vm.levelState === 'fail' && this.vm.level.showFailureMove) {
        const usi =
            this.vm.level.showFailureMove === 'capture'
              ? findCapture(pos)
              : this.vm.level.showFailureMove === 'unprotected'
                ? findUnprotectedCapture(pos)
                : this.vm.level.showFailureMove === 'random'
                  ? findRandomMove(pos)
                  : this.vm.level.showFailureMove(this.vm.level, this.vm.usiCList),
          parsedUsi = usi && parseUsi(usi);

        if (parsedUsi && pos.board.getRole(parsedUsi.to) !== 'king') {
          const usiC = { usi: usi, color: opposite(this.vm.level.color) };
          this.vm.usiCList.push(usiC);
          pos.play(parsedUsi);
          ground.playUsi(this, usiC);
        }
      }

      pos.turn = this.vm.level.color;

      this.shogiground.set({
        turnColor: this.vm.level.color,
        activeColor: active ? this.vm.level.color : undefined,
        checks: !hasObstacles && inCheck(pos),
        movable: {
          dests: active ? (illegalDests ? illegalShogigroundMoveDests(pos) : shogigroundMoveDests(pos)) : new Map(),
        },
        droppable: {
          dests: active ? (illegalDests ? illegalShogigroundDropDests(pos) : shogigroundDropDests(pos)) : new Map(),
        },
        drawable: ground.createDrawable(this.vm.level, this.vm.usiCList),
      });
    }
  }

  onUserMove(orig: Key, dest: Key, promotion: boolean): void {
    const usi = makeUsi({ from: parseSquareName(orig), to: parseSquareName(dest), promotion });
    this.applyUserMoveOrDrop(usi);
  }

  onUserDrop(piece: Piece, key: Key, _promotion: boolean): void {
    const usi = makeUsi({ role: piece.role as Role, to: parseSquareName(key) });
    this.applyUserMoveOrDrop(usi);
  }

  onMove(_orig: Key, _dest: Key, _promotion: boolean, capturedPiece: Piece | undefined): void {
    if (capturedPiece) sound.capture();
    else sound.move();

    if (capturedPiece) {
      this.shogiground.addToHand({ role: capturedPiece.role, color: opposite(capturedPiece.color) });
    }
  }

  onDrop(_piece: Piece, _key: Key): void {
    sound.move();
  }
}
