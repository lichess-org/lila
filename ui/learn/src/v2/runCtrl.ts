import * as sound from '../sound';
import * as stages from '../stage/list';
import { Prop, prop } from 'common';
import { LearnProgress, SnabbdomLearnOpts } from '../learn';
import { Stage } from '../stage/list';
import { LearnCtrl } from './ctrl';
import { clearTimeouts } from '../timeouts';
import { LevelCtrl } from './levelCtrl';

const RESTARTING_KEY = 'learn.restarting';

export interface Vm {
  stageStarting: Prop<boolean>;
  stageCompleted: Prop<boolean>;
}

export class RunCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans;

  level: LevelCtrl;
  vm: Vm;

  constructor(
    ctrl: LearnCtrl,
    readonly opts: SnabbdomLearnOpts,
  ) {
    clearTimeouts();

    this.trans = ctrl.trans;

    const stage = stages.byId[opts.stageId ?? -1];
    if (!stage) {
      // TODO:
      return;
    }
    ctrl.sideCtrl.setStage(stage);

    const levelId =
      opts.levelId ||
      (() => {
        const result = this.data.stages[stage.key];
        let it = 0;
        if (result) while (result.scores[it]) it++;
        if (it >= stage.levels.length) it = 0;
        return it + 1;
        // TODO: also set levelId on opts?
      })();

    this.level = new LevelCtrl(stage.levels[Number(levelId) - 1], {
      onCompleteImmediate: () => {
        opts.storage.saveScore(stage, this.level.blueprint, this.level.vm.score);
      },
      onComplete: () => {
        if (this.level.blueprint.id < stage.levels.length)
          m.route('/' + stage.id + '/' + (this.level.blueprint.id + 1));
        else if (vm.stageCompleted()) return;
        else {
          vm.stageCompleted(true);
          sound.stageEnd();
        }
        m.redraw();
      },
    });

    const isRestarting = site.tempStorage.boolean(RESTARTING_KEY);
    const vm = {
      stageStarting: prop(this.level.blueprint.id === 1 && this.stageScore() === 0 && !isRestarting.get()),
      stageCompleted: prop(false),
    };

    isRestarting.set(false);

    if (vm.stageStarting()) sound.stageStart();
    else this.level.start();
  }

  get stage(): Stage {
    return stages.byId[this.opts.stageId ?? 0]!;
  }

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
    this.level.start();
  };
  restart = () => {
    site.tempStorage.boolean(RESTARTING_KEY).set(true);
    // TODO:
    // m.route('/' + stage.id + '/' + level.blueprint.id);
  };
}
