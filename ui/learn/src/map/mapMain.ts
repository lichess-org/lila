const mapView = require('./mapView');
const stages = require('../stage/list');
const scoring = require('../score');
const timeouts = require('../timeouts');

module.exports = function (opts, trans) {
  return {
    controller: function () {
      timeouts.clearTimeouts();

      opts.stageId = null;
      opts.route = 'map';
      return {
        opts: opts,
        data: opts.storage.data,
        trans: trans,
        isStageIdComplete: function (stageId) {
          const stage = stages.byId[stageId];
          if (!stage) return true;
          const result = opts.storage.data.stages[stage.key];
          if (!result) return false;
          return result.scores.filter(scoring.gtz).length >= stage.levels.length;
        },
        stageProgress: function (stage) {
          const result = opts.storage.data.stages[stage.key];
          const complete = result ? result.scores.filter(scoring.gtz).length : 0;
          return [complete, stage.levels.length];
        },
      };
    },
    view: mapView,
  };
};
