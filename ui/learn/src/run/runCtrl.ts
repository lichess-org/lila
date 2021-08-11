import m from '../mithrilFix';
import * as stages from '../stage/list';
import makeLevel, { LevelCtrl } from '../level';
import { ctrl as makeProgress, Progress } from '../progress';
import * as sound from '../sound';
import * as timeouts from '../timeouts';
import { LearnOpts } from '../main';

export interface Ctrl {
  opts: LearnOpts;
  stage: stages.Stage;
  level: LevelCtrl;
  vm: Vm;
  progress: Progress;
  stageScore(): number;
  getNext(): stages.Stage;
  hideStartingPane(): void;
  restart(): void;
  trans: Trans;
}

export interface Vm {
  stageStarting: _mithril.MithrilBasicProperty<boolean>;
  stageCompleted: _mithril.MithrilBasicProperty<boolean>;
}

export default function (opts: LearnOpts, trans: Trans): Ctrl {
  timeouts.clearTimeouts();

  const stage = stages.byId[Number(m.route.param('stage'))];
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

  const level = makeLevel(stage.levels[Number(levelId) - 1], {
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

  const vm = {
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
}
