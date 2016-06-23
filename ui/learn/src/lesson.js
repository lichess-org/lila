var stageBuilder = require('./stage');

module.exports = function(blueprint, opts) {

  var makeStage = function(id) {
    var stageBlueprint = blueprint.stages[id - 1];
    if (stageBlueprint) return stageBuilder(stageBlueprint, {
      onComplete: opts.onStageComplete
    })
  }

  var stage = makeStage(1);

  return {
    stage: function() {
      return stage;
    },
    next: function() {
      var s = makeStage(stage.blueprint.id + 1);
      if (s) {
        stage = s;
        return true;
      }
      return false;
    }
  }
};
