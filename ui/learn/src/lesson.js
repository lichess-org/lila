var m = require('mithril');
var stageBuilder = require('./stage');
var makeProgress = require('./progress').ctrl;
var sound = require('./sound');

module.exports = function(blueprint, opts) {

  var onStageComplete = function() {
    progress.inc();
    var s = makeStage(stage.blueprint.id + 1);
    if (s) {
      stage = s;
    }
    else {
      vm.completed = true;
      sound.lessonEnd();
      opts.setScore(blueprint, vm.score);
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
      onComplete: onStageComplete,
      restart: restartStage
    })
  }

  var restartStage = function() {
    vm.score = vm.score - stage.vm.score;
    stage = makeStage(stage.blueprint.id);
    m.redraw();
  };

  var stage = makeStage(opts.stage || 1);

  var progress = makeProgress({
    steps: blueprint.stages.length,
    step: stage.blueprint.id
  });

  var vm = {
    score: 0,
    starting: stage.blueprint.id === 1,
    completed: false
  };

  return {
    blueprint: blueprint,
    progress: progress,
    vm: vm,
    stage: function() {
      return stage;
    },
    start: function() {
      vm.starting = false;
    },
    restartStage: restartStage
  }
};
