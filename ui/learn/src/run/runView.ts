import { type Classes, type VNode } from 'snabbdom';

import { bind, button, div, h2, img, p } from 'lib/view';

import chessground from '../chessground';
import type { LearnCtrl } from '../ctrl';
import type { LevelCtrl } from '../levelCtrl';
import { mapSideView } from '../mapSideView';
import { makeStars, progressView } from '../progressView';
import { promotionView } from '../promotionView';
import { withLinebreaks } from '../util';
import congrats from './congrats';
import type { RunCtrl } from './runCtrl';
import stageComplete from './stageComplete';
import stageStarting from './stageStarting';

const renderFailed = (ctrl: RunCtrl): VNode =>
  div('.result.failed', { hook: bind('click', ctrl.restart) }, [
    h2(i18n.learn.puzzleFailed),
    button(i18n.learn.retry),
  ]);

const renderCompleted = (level: LevelCtrl): VNode =>
  div(
    '.result.completed',
    {
      class: { next: !!level.blueprint.nextButton },
      hook: bind('click', level.onComplete),
    },
    [
      h2(congrats()),
      level.blueprint.nextButton ? button(i18n.learn.next) : makeStars(level.blueprint, level.vm.score),
    ],
  );

export const runView = (ctrl: LearnCtrl) => {
  const runCtrl = ctrl.runCtrl;
  const { stage, levelCtrl } = runCtrl;
  const rootClass: Classes = {
    starting: !!levelCtrl.vm.starting,
    completed: levelCtrl.vm.completed && !levelCtrl.blueprint.nextButton,
    'last-step': levelCtrl.vm.lastStep,
    'piece-values': !!levelCtrl.blueprint.showPieceValues,
  };
  if (stage.cssClass) rootClass[stage.cssClass] = true;
  if (levelCtrl.blueprint.cssClass) rootClass[levelCtrl.blueprint.cssClass] = true;
  return div('.learn.learn--run', { class: rootClass }, [
    div('.learn__side', mapSideView(ctrl)),
    div('.learn__main.main-board', { class: { apples: levelCtrl.isAppleLevel() } }, [
      runCtrl.stageStarting() ? stageStarting(runCtrl) : null,
      runCtrl.stageCompleted() ? stageComplete(runCtrl) : null,
      chessground(ctrl.runCtrl),
      promotionView(ctrl.runCtrl),
    ]),
    div('.learn__table', [
      div('.wrap', [
        div('.title', [
          img(stage.image, '')(),
          div('.text', [h2(stage.title), p('.subtitle', stage.subtitle)]),
        ]),
        levelCtrl.vm.failed
          ? renderFailed(runCtrl)
          : levelCtrl.vm.completed
            ? renderCompleted(levelCtrl)
            : div('.goal', withLinebreaks(levelCtrl.blueprint.goal)),
        progressView(runCtrl),
      ]),
    ]),
  ]);
};
