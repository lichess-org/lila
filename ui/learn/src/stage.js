var m = require('mithril');
var levelBuilder = require('./level');
var makeProgress = require('./progress').ctrl;
var sound = require('./sound');

module.exports = function(blueprint, opts) {

  var onLevelComplete = function() {
    opts.saveScore(blueprint, level.blueprint, vm.score);
    progress.inc();
    var s = makeLevel(level.blueprint.id + 1);
    if (s) {
      level = s;
    }
    else {
      vm.completed = true;
      sound.stageEnd();
    }
    m.redraw();
  };

  var makeLevel = function(id) {
    var levelBlueprint = blueprint.levels[id - 1];
    if (levelBlueprint) return levelBuilder(levelBlueprint, {
      onScore: function(score) {
        vm.score += score;
        m.redraw();
      },
      onComplete: onLevelComplete,
      restart: restartLevel
    })
  }

  var restartLevel = function() {
    vm.score = vm.score - level.vm.score;
    level = makeLevel(level.blueprint.id);
    m.redraw();
  };

  var level = makeLevel(opts.level || 1);

  var progress = makeProgress({
    steps: blueprint.levels.length,
    step: level.blueprint.id
  });

  var vm = {
    score: 0,
    starting: level.blueprint.id === 1,
    completed: false
  };
  sound.stageStart();

  return {
    blueprint: blueprint,
    progress: progress,
    vm: vm,
    level: function() {
      return level;
    },
    start: function() {
      vm.starting = false;
    },
    restartLevel: restartLevel
  }
};
