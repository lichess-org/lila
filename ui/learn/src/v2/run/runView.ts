import * as util from '../util';
import chessground from '../chessground';
import congrats from './congrats';
import stageStarting from './stageStarting';
import stageComplete from './stageComplete';
import { view as renderPromotion } from '../promotion';
import { LevelCtrl } from '../levelCtrl';
import { RunCtrl } from './runCtrl';
import { mapSideView } from '../mapSideView';
import { LearnCtrl } from '../ctrl';
import { h } from 'snabbdom';
import { makeStars, progressView } from '../progressView';

function renderFailed(ctrl: RunCtrl) {
  return h(
    'div.result.failed',
    {
      onclick: ctrl.restart,
    },
    [h('h2', ctrl.trans.noarg('puzzleFailed')), h('button', ctrl.trans.noarg('retry'))],
  );
}

function renderCompleted(ctrl: RunCtrl, level: LevelCtrl) {
  return h(
    'div.result.completed',
    {
      class: { next: !!level.blueprint.nextButton },
      onclick: level.onComplete,
    },
    [
      h('h2', ctrl.trans.noarg(congrats())),
      level.blueprint.nextButton
        ? h('button', ctrl.trans.noarg('next'))
        : makeStars(level.blueprint, level.vm.score),
    ],
  );
}

export const runView = (ctrl: LearnCtrl) => {
  const runCtrl = ctrl.runCtrl;
  const stage = runCtrl.stage;
  const level = runCtrl.level;

  return h(
    `div.learn.learn--run.${stage.cssClass}.${level.blueprint.cssClass}`,
    {
      class: {
        starting: !!level.vm.starting,
        completed: level.vm.completed && !level.blueprint.nextButton,
        'last-step': !!level.vm.lastStep,
        'piece-values': !!level.blueprint.showPieceValues,
      },
    },
    [
      h('div.learn__side', mapSideView(ctrl)),
      h('div.learn__main.main-board', [
        runCtrl.vm.stageStarting() ? stageStarting(runCtrl) : null,
        runCtrl.vm.stageCompleted() ? stageComplete(runCtrl) : null,
        // TODO:
        // chessground.view(ground.instance),
        chessground(ctrl.runCtrl),
        renderPromotion(runCtrl, level),
      ]),
      h('div.learn__table', [
        h('div.wrap', [
          h('div.title', [
            h('img', { attrs: { src: stage.image } }),
            h('div.text', [
              h('h2', ctrl.trans.noarg(stage.title)),
              h('p.subtitle', ctrl.trans.noarg(stage.subtitle)),
            ]),
          ]),
          level.vm.failed
            ? renderFailed(runCtrl)
            : level.vm.completed
            ? renderCompleted(runCtrl, level)
            : h('div.goal', util.withLinebreaks(ctrl.trans.noarg(level.blueprint.goal))),
          progressView(runCtrl),
        ]),
      ]),
    ],
  );
};
