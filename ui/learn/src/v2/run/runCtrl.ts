import * as sound from '../sound';
import * as stages from '../stage/list';
import { Prop, prop } from 'common';
import { LearnProgress, LearnOpts } from '../../learn';
import { Stage } from '../stage/list';
import { LearnCtrl } from '../ctrl';
import { clearTimeouts } from '../timeouts';
import { LevelCtrl } from '../levelCtrl';

const RESTARTING_KEY = 'learn.restarting';

export interface Vm {
  stageStarting: Prop<boolean>;
  stageCompleted: Prop<boolean>;
}

export class RunCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans;

  chessground: CgApi | undefined;

  levelCtrl: LevelCtrl;
  vm: Vm;

  get stage(): Stage {
    return stages.byId[this.opts.stageId ?? 1]!;
  }

  constructor(
    ctrl: LearnCtrl,
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.trans = ctrl.trans;

    const levelId =
      this.opts.levelId ||
      (() => {
        const result = this.data.stages[this.stage.key];
        let it = 0;
        if (result) while (result.scores[it]) it++;
        if (it >= this.stage.levels.length) it = 0;
        const newLevelId = it + 1;
        this.opts.levelId = newLevelId;
        return newLevelId;
      })();

    this.levelCtrl = new LevelCtrl(
      this.stage.levels[Number(levelId) - 1],
      {
        onCompleteImmediate: () => {
          this.opts.storage.saveScore(this.stage, this.levelCtrl!.blueprint, this.levelCtrl!.vm.score);
        },
        onComplete: () => {
          if (this.levelCtrl!.blueprint.id < this.stage.levels.length) {
            // TODO:
            // m.route('/' + stage.id + '/' + (this.level.blueprint.id + 1));
          } else if (this.vm.stageCompleted()) return;
          else {
            this.vm.stageCompleted(true);
            sound.stageEnd();
          }

          this.redraw();
        },
      },
      this,
      this.redraw,
    );

    const isRestarting = site.tempStorage.boolean(RESTARTING_KEY);
    this.vm = {
      stageStarting: prop(
        this.levelCtrl.blueprint.id === 1 && this.stageScore() === 0 && !isRestarting.get(),
      ),
      stageCompleted: prop(false),
    };

    isRestarting.set(false);

    if (this.vm.stageStarting()) sound.stageStart();
    else this.levelCtrl.start();
  }

  setChessground = (chessground: CgApi) => {
    this.chessground = chessground;
    this.levelCtrl.initializeWithCg();
  };

  stageScore = () => {
    const res = this.data.stages[this.stage.key];
    return res?.scores.reduce((a, b) => a + b) ?? 0;
  };

  score = (level: stages.Level) => {
    return this.data.stages[this.stage.key] ? this.data.stages[this.stage.key].scores[level.id - 1] : 0;
  };
  getNext = () => stages.byId[this.stage.id + 1];
  hideStartingPane = () => {
    if (!this.vm.stageStarting()) return;
    this.vm.stageStarting(false);
    this.levelCtrl?.start();
  };
  restart = () => {
    site.tempStorage.boolean(RESTARTING_KEY).set(true);
    // TODO:
    // m.route('/' + stage.id + '/' + level.blueprint.id);
  };
}
