import { LearnProgress, SnabbdomLearnOpts } from '../learn';
import { Stage } from '../stage/list';
import * as scoring from '../score';
import { SnabbdomSideCtrl } from './sideCtrl';
import { clearTimeouts } from '../timeouts';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans = site.trans(this.opts.i18n);

  sideCtrl: SnabbdomSideCtrl;

  constructor(
    readonly opts: SnabbdomLearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.sideCtrl = new SnabbdomSideCtrl(this, opts);
  }

  isStageIdComplete = (id: number) => false && id;

  stageProgress = (stage: Stage) => {
    const result = this.data.stages[stage.key];
    const complete = result ? result.scores.filter(scoring.gtz).length : 0;
    return [complete, stage.levels.length];
  };
}
