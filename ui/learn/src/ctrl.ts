import { LearnProgress, LearnOpts } from './learn';
import { Stage } from './stage/list';
import * as stages from './stage/list';
import * as scoring from './score';
import { SideCtrl } from './sideCtrl';
import { clearTimeouts } from './timeouts';
import { extractHashParameters } from './hashRouting';
import { RunCtrl } from './run/runCtrl';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans = site.trans(this.opts.i18n);

  sideCtrl: SideCtrl;
  runCtrl: RunCtrl;

  constructor(
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.setStageLevelFromHash();

    this.sideCtrl = new SideCtrl(this, opts);
    this.runCtrl = new RunCtrl(this, opts, redraw);

    window.addEventListener('hashchange', () => {
      this.setStageLevelFromHash();
      this.sideCtrl.updateCategId();
      this.runCtrl.initializeLevel();
      this.redraw();
    });
  }

  setStageLevelFromHash = () => {
    const { stageId, levelId } = extractHashParameters();
    this.opts.stageId = stageId;
    this.opts.levelId =
      // use the level id from the hash path if it exists
      levelId ||
      // otherwise find a level id based on the last completed level
      (() => {
        if (!stageId) return;
        const stage = stages.byId[stageId];
        if (!stage) return;
        const result = this.data.stages[stage.key];
        let it = 0;
        if (result) while (result.scores[it]) it++;
        if (it >= stage.levels.length) it = 0;
        const newLevelId = it + 1;
        this.opts.levelId = newLevelId;
        return newLevelId;
      })() ||
      // finally, fallback to a level id of 1
      1;
  };

  inStage = () => this.opts.stageId !== null;

  isStageIdComplete = (stageId: number) => {
    const stage = stages.byId[stageId];
    if (!stage) return true;
    const result = this.data.stages[stage.key];
    if (!result) return false;
    return result.scores.filter(scoring.gtz).length >= stage.levels.length;
  };

  stageProgress = (stage: Stage) => {
    const result = this.data.stages[stage.key];
    const complete = result ? result.scores.filter(scoring.gtz).length : 0;
    return [complete, stage.levels.length];
  };
}
