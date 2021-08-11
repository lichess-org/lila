import m from '../mithrilFix';
import * as chessground from 'chessground';
import * as util from '../util';
import * as ground from '../ground';
import congrats from '../congrats';
import stageStarting from './stageStarting';
import stageComplete from './stageComplete';
import { view as renderPromotion } from '../promotion';
import { makeStars, view as renderProgress } from '../progress';
import { Ctrl } from './runCtrl';
import { LevelCtrl } from '../level';

function renderFailed(ctrl: Ctrl) {
  return m(
    'div.result.failed',
    {
      onclick: ctrl.restart,
    },
    [m('h2', ctrl.trans.noarg('puzzleFailed')), m('button', ctrl.trans.noarg('retry'))]
  );
}

function renderCompleted(ctrl: Ctrl, level: LevelCtrl) {
  return m(
    'div.result.completed',
    {
      class: level.blueprint.nextButton ? 'next' : '',
      onclick: level.onComplete,
    },
    [
      m('h2', ctrl.trans.noarg(congrats())),
      level.blueprint.nextButton ? m('button', ctrl.trans.noarg('next')) : makeStars(level.blueprint, level.vm.score),
    ]
  );
}

export default function (ctrl: Ctrl) {
  const stage = ctrl.stage;
  const level = ctrl.level;

  return m(
    'div',
    {
      class:
        'learn learn--run ' +
        stage.cssClass +
        ' ' +
        level.blueprint.cssClass +
        (level.vm.starting ? ' starting' : '') +
        (level.vm.completed && !level.blueprint.nextButton ? ' completed' : '') +
        (level.vm.lastStep ? ' last-step' : '') +
        (level.blueprint.showPieceValues ? ' piece-values' : ''),
    },
    [
      m('div.learn__side', ctrl.opts.side.view()),
      m('div.learn__main.main-board', [
        ctrl.vm.stageStarting() ? stageStarting(ctrl) : null,
        ctrl.vm.stageCompleted() ? stageComplete(ctrl) : null,
        (chessground as any).view(ground.instance),
        renderPromotion(ctrl, level),
      ]),
      m('div.learn__table', [
        m('div.wrap', [
          m('div.title', [
            m('img', {
              src: stage.image,
            }),
            m('div.text', [m('h2', ctrl.trans.noarg(stage.title)), m('p.subtitle', ctrl.trans.noarg(stage.subtitle))]),
          ]),
          level.vm.failed
            ? renderFailed(ctrl)
            : level.vm.completed
            ? renderCompleted(ctrl, level)
            : m('div.goal', util.withLinebreaks(ctrl.trans.noarg(level.blueprint.goal))),
          renderProgress(ctrl.progress),
        ]),
      ]),
    ]
  );
}
