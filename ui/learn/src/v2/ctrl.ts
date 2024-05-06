import { LearnOpts, LearnProgress } from '../learn';
import { Stage } from '../stage/list';
import * as scoring from '../score';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans = site.trans(this.opts.i18n);

  constructor(
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {}

  isStageIdComplete = (id: number) => false && id;

  stageProgress = (stage: Stage) => {
    const result = this.data.stages[stage.key];
    const complete = result ? result.scores.filter(scoring.gtz).length : 0;
    return [complete, stage.levels.length];
  };
}
