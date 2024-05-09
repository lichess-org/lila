// import * as util from '../util';
import * as stages from './stage/list';
import * as scoring from './score';
import { Prop, prop } from 'common';
import { LearnProgress, LearnOpts } from './learn';
import { Stage } from './stage/list';
import { LearnCtrl } from './ctrl';

export class SnabbdomSideCtrl {
  opts: LearnOpts;
  trans: Trans;
  data: LearnProgress;

  categId: Prop<number> = prop(0);

  constructor(ctrl: LearnCtrl, opts: LearnOpts) {
    this.opts = opts;
    this.trans = ctrl.trans;
    this.data = ctrl.data;
  }

  reset = () => this.opts.storage.reset();
  activeStageId = () => this.opts.stageId;
  setStage = (stage: Stage) => {
    this.categId(stages.stageIdToCategId(stage.id) || this.categId());
  };
  progress = () => {
    const max = stages.list.length * 10;
    const data = this.data.stages;
    const total = Object.keys(data).reduce(function (t, key) {
      const rank = scoring.getStageRank(stages.byKey[key], data[key].scores);
      if (rank === 1) return t + 10;
      if (rank === 2) return t + 8;
      return t + 5;
    }, 0);
    return Math.round((total / max) * 100);
  };
}
