// import * as util from '../util';
// import chessground from 'chessground';
// import * as ground from '../ground';
// import congrats from '../congrats';
// import stageStarting from '../run/stageStarting';
// import stageComplete from '../run/stageComplete';
// import { view as renderPromotion } from '../promotion';
// import { LevelCtrl } from '../level';
// import { RunCtrl } from './runCtrl';
import { mapSideView } from './mapSideView';
import { LearnCtrl } from './ctrl';
import { h } from 'snabbdom';
// import { makeStars } from './progressView';

// function renderFailed(ctrl: RunCtrl) {
//   return h(
//     'div.result.failed',
//     {
//       onclick: ctrl.restart,
//     },
//     [h('h2', ctrl.trans.noarg('puzzleFailed')), h('button', ctrl.trans.noarg('retry'))],
//   );
// }

// function renderCompleted(ctrl: RunCtrl, level: LevelCtrl) {
//   return h(
//     'div.result.completed',
//     {
//       class: { next: !!level.blueprint.nextButton },
//       onclick: level.onComplete,
//     },
//     [
//       h('h2', ctrl.trans.noarg(congrats())),
//       level.blueprint.nextButton
//         ? h('button', ctrl.trans.noarg('next'))
//         : makeStars(level.blueprint, level.vm.score),
//     ],
//   );
// }

export const runView = (ctrl: LearnCtrl) => {
  // const runCtrl = ctrl.runCtrl;
  // const stage = runCtrl.stage;
  // const level = ctrl.level;

  return h(
    'div',
    // {
    //   class:
    //     'learn learn--run ' +
    //     stage.cssClass +
    //     ' ' +
    //     level.blueprint.cssClass +
    //     (level.vm.starting ? ' starting' : '') +
    //     (level.vm.completed && !level.blueprint.nextButton ? ' completed' : '') +
    //     (level.vm.lastStep ? ' last-step' : '') +
    //     (level.blueprint.showPieceValues ? ' piece-values' : ''),
    // },
    [
      h('div.learn__side', mapSideView(ctrl)),
      h('div.learn__main.main-board', [
        // ctrl.vm.stageStarting() ? stageStarting(ctrl) : null,
        // ctrl.vm.stageCompleted() ? stageComplete(ctrl) : null,
        // chessground.view(ground.instance),
        // renderPromotion(ctrl, level),
      ]),
      h('div.learn__table', [
        h('div.wrap', [
          h('div.title', [
            h('img', {
              // src: stage.image,
            }),
            h('div.text', [
              // h('h2', ctrl.trans.noarg(stage.title)),
              // h('p.subtitle', ctrl.trans.noarg(stage.subtitle)),
            ]),
          ]),
          // level.vm.failed
          //   ? renderFailed(ctrl)
          //   : level.vm.completed
          //   ? renderCompleted(ctrl, level)
          //   : h('div.goal', util.withLinebreaks(ctrl.trans.noarg(level.blueprint.goal))),
          // renderProgress(ctrl.progress),
        ]),
      ]),
    ],
  );
};
