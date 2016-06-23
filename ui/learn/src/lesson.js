var m = require('mithril');
var stageBuilder = require('./stage');

module.exports = function(blueprint, opts) {

  var makeStage = function(id) {
    var stageBlueprint = blueprint.stages[id - 1];
    if (stageBlueprint) return stageBuilder(stageBlueprint, {
      onScore: function(score) {
        vm.score += score;
        m.redraw();
      },
      onComplete: opts.onStageComplete
    })
  }

  var stage = makeStage(1);

  var vm = {
    score: 0
  };

  return {
    blueprint: blueprint,
    vm: vm,
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
