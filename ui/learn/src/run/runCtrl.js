var m = require('mithril');
var stages = require('../stage/list');
var makeLevel = require('../level');
var makeProgress = require('../progress').ctrl;
var sound = require('../sound');

module.exports = function(opts) {

  var stage = stages.byId[m.route.param('stage')];
  if (!stage) m.route('/');
  opts.setStage(stage);

  var level = makeLevel(stage.levels[(m.route.param('level') || 1) - 1], {
    stage: stage,
    onComplete: function() {
      opts.storage.saveScore(stage, level.blueprint, level.vm.score);
      if (level.blueprint.id < stage.levels.length)
        m.route('/' + stage.id + '/' + (level.blueprint.id + 1));
      else {
        vm.stageCompleted(true);
        sound.stageEnd();
      }
      m.redraw();
    }
  });

  var stageScore = function() {
    var res = opts.storage.data.stages[stage.key];
    return res ? res.scores.reduce(function(a, b) {
      return a + b;
    }) : 0;
  };

  opts.route = 'run';
  opts.stageId = stage.id;

  var vm = {
    stageStarting: m.prop(level.blueprint.id === 1 && stageScore() === 0),
    stageCompleted: m.prop(false)
  };

  var getNext = function() {
    return stages.byId[stage.id + 1];
  };
  if (vm.stageStarting()) sound.stageStart();
  else level.start();

  return {
    stage: stage,
    level: level,
    vm: vm,
    progress: makeProgress(stage, level, opts.storage.data),
    stageScore: stageScore,
    getNext: getNext,
    hideStartingPane: function() {
      if (!vm.stageStarting()) return;
      vm.stageStarting(false);
      level.start();
    },
    restart: function() {
      m.route('/' + stage.id + '/' + level.blueprint.id);
    }
  };
};
