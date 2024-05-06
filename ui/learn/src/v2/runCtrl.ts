// import * as util from '../util';
import * as stages from '../stage/list';
// import * as scoring from '../score';
import { Prop } from 'common';
import { SnabbdomLearnOpts } from '../learn';
// import { ctrl as makeProgress, Progress } from '../progress';
import { Stage } from '../stage/list';
import { LearnCtrl } from './ctrl';
import { clearTimeouts } from '../timeouts';

export interface Vm {
  stageStarting: Prop<boolean>;
  stageCompleted: Prop<boolean>;
}

export class RunCtrl {
  trans: Trans;

  stage: Stage;
  // level: LevelCtrl;
  vm: Vm;
  // progress: Progress;

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

    // const levelId =
    //   opts.levelId ||
    //   (function () {
    //     const result = opts.storage.data.stages[stage.key];
    //     let it = 0;
    //     if (result) while (result.scores[it]) it++;
    //     if (it >= stage.levels.length) it = 0;
    //     return it + 1;
    //     // TODO: also set levelId on opts?
    //   })();

    // const level = makeLevel(stage.levels[Number(levelId) - 1], {
    //   onCompleteImmediate() {
    //     opts.storage.saveScore(stage, level.blueprint, level.vm.score);
    //   },
    //   onComplete() {
    //     if (level.blueprint.id < stage.levels.length)
    //       m.route('/' + stage.id + '/' + (level.blueprint.id + 1));
    //     else if (vm.stageCompleted()) return;
    //     else {
    //       vm.stageCompleted(true);
    //       sound.stageEnd();
    //     }
    //     m.redraw();
    //   },
    // });

    //   const vm = {
    //   stageStarting: m.prop(level.blueprint.id === 1 && stageScore() === 0 && !isRestarting.get()),
    //   stageCompleted: m.prop(false),
    // };

    site.tempStorage.boolean('learn.restarting').set(false);

    // if (vm.stageStarting()) sound.stageStart();
    // else level.start();

    // TODO:
    // m.redraw.strategy('diff');
  }

  stageScore = () => {
    const res = this.opts.storage.data.stages[this.stage.key];
    return res?.scores.reduce((a, b) => a + b) ?? 0;
  };
  getNext = () => stages.byId[this.stage.id + 1];
  hideStartingPane = () => {
    if (!this.vm.stageStarting()) return;
    this.vm.stageStarting(false);
    // this.level.start();
  };
  restart = () => {
    site.tempStorage.boolean('learn.restarting').set(true);
    // m.route('/' + stage.id + '/' + level.blueprint.id);
  };
}
