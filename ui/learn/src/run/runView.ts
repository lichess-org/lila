import { withLinebreaks } from '../util';
import chessground from '../chessground';
import congrats from './congrats';
import stageStarting from './stageStarting';
import stageComplete from './stageComplete';
import type { LevelCtrl } from '../levelCtrl';
import type { RunCtrl } from './runCtrl';
import { mapSideView } from '../mapSideView';
import type { LearnCtrl } from '../ctrl';
import { h, type Classes, type VNode } from 'snabbdom';
import { bind } from 'lib/view';
import { makeStars, progressView } from '../progressView';
import { promotionView } from '../promotionView';

const renderFailed = (ctrl: RunCtrl): VNode =>
  h('div.result.failed', { hook: bind('click', ctrl.restart) }, [
    h('h2', i18n.learn.puzzleFailed),
    h('button', i18n.learn.retry),
  ]);

const renderCompleted = (level: LevelCtrl): VNode =>
  h(
    'div.result.completed',
    {
      class: { next: !!level.blueprint.nextButton },
      hook: bind('click', level.onComplete),
    },
    [
      h('h2', congrats()),
      level.blueprint.nextButton ? h('button', i18n.learn.next) : makeStars(level.blueprint, level.vm.score),
    ],
  );

export const runView = (ctrl: LearnCtrl) => {
  const runCtrl = ctrl.runCtrl;
  const { stage, levelCtrl } = runCtrl;
  const rootClass: Classes = {
    starting: !!levelCtrl.vm.starting,
    completed: levelCtrl.vm.completed && !levelCtrl.blueprint.nextButton,
    'last-step': !!levelCtrl.vm.lastStep,
    'piece-values': !!levelCtrl.blueprint.showPieceValues,
  };
  if (stage.cssClass) rootClass[stage.cssClass] = true;
  if (levelCtrl.blueprint.cssClass) rootClass[levelCtrl.blueprint.cssClass] = true;
  return h(
    'div.learn.learn--run',
    {
      class: rootClass,
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
            h('div.text', [h('h2', stage.title), h('p.subtitle', stage.subtitle)]),
          ]),
          levelCtrl.vm.failed
            ? renderFailed(runCtrl)
            : levelCtrl.vm.completed
              ? renderCompleted(levelCtrl)
              : h('div.goal', withLinebreaks(levelCtrl.blueprint.goal)),
          progressView(runCtrl),
        ]),
      ]),
    ],
  );
};
