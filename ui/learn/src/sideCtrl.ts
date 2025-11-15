import { stageIdToCategId, byKey as stageByKey, list as stageList } from './stage/list';
import * as scoring from './score';
import { type Prop, propWithEffect } from 'lib';
import type { LearnProgress, LearnOpts } from './learn';
import { LearnCtrl } from './ctrl';

export class SideCtrl {
  opts: LearnOpts;
  data: LearnProgress;

  categId: Prop<number>;

  constructor(ctrl: LearnCtrl, opts: LearnOpts) {
    this.opts = opts;
    this.data = ctrl.data;

    this.categId = propWithEffect(this.getCategIdFromStageId() ?? 0, ctrl.redraw);
  }

  reset = () => this.opts.storage.reset();

  activeStageId = () => this.opts.stageId || 1;
  getCategIdFromStageId = () => stageIdToCategId(this.activeStageId());
  updateCategId = () => this.categId(this.getCategIdFromStageId() || this.categId());

  progress = () => {
    const max = stageList.length * 10;
    const data = this.data.stages;
    const total = Object.keys(data).reduce((t, key) => {
      const rank = scoring.getStageRank(stageByKey[key], data[key].scores);
      if (rank === 1) return t + 10;
      if (rank === 2) return t + 8;
      return t + 5;
    }, 0);
    return Math.round((total / max) * 100);
  };
}
