import * as stages from './stage/list';
import * as scoring from './score';
import { Prop, propWithEffect } from 'common';
import { LearnProgress, LearnOpts } from './learn';
import { LearnCtrl } from './ctrl';

export class SideCtrl {
  opts: LearnOpts;
  trans: Trans;
  data: LearnProgress;

  categId: Prop<number>;

  constructor(ctrl: LearnCtrl, opts: LearnOpts) {
    this.opts = opts;
    this.trans = ctrl.trans;
    this.data = ctrl.data;

    this.categId = propWithEffect(this.getCategIdFromStageId() ?? 0, ctrl.redraw);
  }

  reset = () => this.opts.storage.reset();

  activeStageId = () => this.opts.stageId || 1;
  getCategIdFromStageId = () => stages.stageIdToCategId(this.activeStageId());
  updateCategId = () => this.categId(this.getCategIdFromStageId() || this.categId());

  progress = () => {
    const max = stages.list.length * 10;
    const data = this.data.stages;
    const total = Object.keys(data).reduce((t, key) => {
      const rank = scoring.getStageRank(stages.byKey[key], data[key].scores);
      if (rank === 1) return t + 10;
      if (rank === 2) return t + 8;
      return t + 5;
    }, 0);
    return Math.round((total / max) * 100);
  };
}
