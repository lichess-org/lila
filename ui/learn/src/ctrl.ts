import type { LearnProgress, LearnOpts } from './learn';
import { type Stage, byId as stageById } from './stage/list';
import { gtz } from './score';
import { SideCtrl } from './sideCtrl';
import { clearTimeouts } from './timeouts';
import { extractHashParameters } from './hashRouting';
import { RunCtrl } from './run/runCtrl';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;

  sideCtrl: SideCtrl;
  runCtrl: RunCtrl;

  constructor(
    readonly opts: LearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.setStageLevelFromHash();

    this.sideCtrl = new SideCtrl(this, opts);
    this.runCtrl = new RunCtrl(opts, redraw);

    window.addEventListener('hashchange', () => {
      this.setStageLevelFromHash();
      this.sideCtrl.updateCategId();
      if (this.opts.stageId !== null) this.runCtrl.initializeLevel();
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
        const stage = stageById[stageId];
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
    const stage = stageById[stageId];
    if (!stage) return true;
    const result = this.data.stages[stage.key];
    if (!result) return false;
    return result.scores.filter(gtz).length >= stage.levels.length;
  };

  stageProgress = (stage: Stage) => {
    const result = this.data.stages[stage.key];
    const complete = result ? result.scores.filter(gtz).length : 0;
    return [complete, stage.levels.length];
  };
}
