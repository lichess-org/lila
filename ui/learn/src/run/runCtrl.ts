const m = require('mithril');
const stages = require('../stage/list');
const makeLevel = require('../level');
const makeProgress = require('../progress').ctrl;
const sound = require('../sound');
const timeouts = require('../timeouts');

module.exports = function (opts, trans) {
  timeouts.clearTimeouts();

  const stage = stages.byId[m.route.param('stage')];
  if (!stage) m.route('/');
  opts.side.ctrl.setStage(stage);

  const levelId =
    m.route.param('level') ||
    (function () {
      const result = opts.storage.data.stages[stage.key];
      let it = 0;
      if (result) while (result.scores[it]) it++;
      if (it >= stage.levels.length) it = 0;
      return it + 1;
    })();

  var level = makeLevel(stage.levels[levelId - 1], {
    stage: stage,
    onCompleteImmediate() {
      opts.storage.saveScore(stage, level.blueprint, level.vm.score);
    },
    onComplete() {
      if (level.blueprint.id < stage.levels.length) m.route('/' + stage.id + '/' + (level.blueprint.id + 1));
      else if (vm.stageCompleted()) return;
      else {
        vm.stageCompleted(true);
        sound.stageEnd();
      }
      m.redraw();
    },
  });

  const stageScore = function () {
    const res = opts.storage.data.stages[stage.key];
    return res
      ? res.scores.reduce(function (a, b) {
          return a + b;
        })
      : 0;
  };

  opts.route = 'run';
  opts.stageId = stage.id;

  const isRestarting = lichess.tempStorage.makeBoolean('learn.restarting');

  var vm = {
    stageStarting: m.prop(level.blueprint.id === 1 && stageScore() === 0 && !isRestarting.get()),
    stageCompleted: m.prop(false),
  };

  isRestarting.set(false);

  const getNext = function () {
    return stages.byId[stage.id + 1];
  };
  if (vm.stageStarting()) sound.stageStart();
  else level.start();

  // setTimeout(function() {
  //   if (level.blueprint.id < stage.levels.length)
  //     m.route('/' + stage.id + '/' + (level.blueprint.id + 1));
  //   else if (getNext()) m.route('/' + (getNext().id));
  // }, 1500);

  m.redraw.strategy('diff');

  return {
    opts: opts,
    stage: stage,
    level: level,
    vm: vm,
    progress: makeProgress(stage, level, opts.storage.data),
    stageScore: stageScore,
    getNext: getNext,
    hideStartingPane: function () {
      if (!vm.stageStarting()) return;
      vm.stageStarting(false);
      level.start();
    },
    restart: function () {
      isRestarting.set(true);
      m.route('/' + stage.id + '/' + level.blueprint.id);
    },
    trans: trans,
  };
};
