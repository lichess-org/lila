import * as util from '../util';
import chessground from '../chessground';
import congrats from './congrats';
import stageStarting from './stageStarting';
import stageComplete from './stageComplete';
import { LevelCtrl } from '../levelCtrl';
import { RunCtrl } from './runCtrl';
import { mapSideView } from '../mapSideView';
import { LearnCtrl } from '../ctrl';
import { h, VNode } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { makeStars, progressView } from '../progressView';
import { promotionView } from '../promotionView';

const renderFailed = (ctrl: RunCtrl): VNode =>
  h('div.result.failed', { hook: bind('click', ctrl.restart) }, [
    h('h2', ctrl.trans.noarg('puzzleFailed')),
    h('button', ctrl.trans.noarg('retry')),
  ]);

const renderCompleted = (ctrl: RunCtrl, level: LevelCtrl): VNode =>
  h(
    'div.result.completed',
    {
      class: { next: !!level.blueprint.nextButton },
      hook: bind('click', level.onComplete),
    },
    [
      h('h2', ctrl.trans.noarg(congrats())),
      level.blueprint.nextButton
        ? h('button', ctrl.trans.noarg('next'))
        : makeStars(level.blueprint, level.vm.score),
    ],
  );

export const runView = (ctrl: LearnCtrl) => {
  const runCtrl = ctrl.runCtrl;
  const { stage, levelCtrl } = runCtrl;
  return h(
    `div.learn.learn--run.${stage.cssClass}.${levelCtrl.blueprint.cssClass}`,
    {
      class: {
        starting: !!levelCtrl.vm.starting,
        completed: levelCtrl.vm.completed && !levelCtrl.blueprint.nextButton,
        'last-step': !!levelCtrl.vm.lastStep,
        'piece-values': !!levelCtrl.blueprint.showPieceValues,
      },
    },
    [
      h('div.learn__side', mapSideView(ctrl)),
      h('div.learn__main.main-board', { class: { apples: levelCtrl.isAppleLevel() } }, [
        runCtrl.stageStarting() ? stageStarting(runCtrl) : null,
        runCtrl.stageCompleted() ? stageComplete(runCtrl) : null,
        chessground(ctrl.runCtrl),
        promotionView(ctrl.runCtrl),
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
          levelCtrl.vm.failed
            ? renderFailed(runCtrl)
            : levelCtrl.vm.completed
            ? renderCompleted(runCtrl, levelCtrl)
            : h('div.goal', util.withLinebreaks(ctrl.trans.noarg(levelCtrl.blueprint.goal))),
          progressView(runCtrl),
        ]),
      ]),
    ],
  );
};
