var m = require('mithril');
var stageBuilder = require('./stage');
var sound = require('./sound');

module.exports = function(blueprint, opts) {

  var onStageComplete = function() {
    var s = makeStage(stage.blueprint.id + 1);
    if (s) stage = s;
    else {
      vm.completed = true;
      sound.lessonEnd();
    }
    m.redraw();
  };

  var makeStage = function(id) {
    var stageBlueprint = blueprint.stages[id - 1];
    if (stageBlueprint) return stageBuilder(stageBlueprint, {
      onScore: function(score) {
        vm.score += score;
        m.redraw();
      },
      onComplete: onStageComplete
    })
  }

  var stage = makeStage(opts.stage || 1);

  var vm = {
    score: 0,
    starting: stage.blueprint.id === 1,
    completed: false
  };

  return {
    blueprint: blueprint,
    vm: vm,
    stage: function() {
      return stage;
    },
    start: function() {
      vm.starting = false;
    }
  }
};
