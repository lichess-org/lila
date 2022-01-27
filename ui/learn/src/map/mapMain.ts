import mapView from './mapView';
import * as stages from '../stage/list';
import * as scoring from '../score';
import * as timeouts from '../timeouts';
import { LearnOpts, LearnProgress } from '../main';

export interface MapCtrl {
  opts: LearnOpts;
  data: LearnProgress;
  trans: Trans;
  isStageIdComplete(stageId: number): boolean;
  stageProgress(stage: stages.Stage): [number, number];
}

export default function (opts: LearnOpts, trans: Trans) {
  return {
    controller: function (): MapCtrl {
      timeouts.clearTimeouts();

      opts.stageId = null;
      opts.route = 'map';
      return {
        opts: opts,
        data: opts.storage.data,
        trans: trans,
        isStageIdComplete: function (stageId: number) {
          const stage = stages.byId[stageId];
          if (!stage) return true;
          const result = opts.storage.data.stages[stage.key];
          if (!result) return false;
          return result.scores.filter(scoring.gtz).length >= stage.levels.length;
        },
        stageProgress: function (stage: stages.Stage) {
          const result = opts.storage.data.stages[stage.key];
          const complete = result ? result.scores.filter(scoring.gtz).length : 0;
          return [complete, stage.levels.length];
        },
      };
    },
    view: mapView,
  };
}
