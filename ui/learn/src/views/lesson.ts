import { VNode, h } from 'snabbdom';
import LearnCtrl from '../ctrl';
import { Level } from '../interfaces';
import congrats from './congrats';
import completed from './overlays/completed';
import starting from './overlays/starting';
import side from './side';

const star = h('i', { attrs: { 'data-icon': 't' } });

function makeStars(score: number): VNode {
  const stars = [];
  for (let i = 0; i < score; i++) stars.push(star);
  return h('span.stars.st' + stars.length, stars);
}

function progress(ctrl: LearnCtrl) {
  const vm = ctrl.vm!;
  return h(
    'div.progress',
    vm.stage.levels.map(function (level: Level) {
      const score = ctrl.progress.get(vm.stage.key)[level.id - 1];
      const status = level.id === vm.level.id ? 'active' : score ? 'done' : 'future';
      const label = score ? makeStars(score) : h('span.id', level.id);
      return h(
        'a.' + status,
        {
          on: {
            click: () => {
              ctrl.setLesson(vm.stage.id, level.id);
              ctrl.redraw();
            },
          },
        },
        label
      );
    })
  );
}

function renderFailed(ctrl: LearnCtrl) {
  return h(
    'div.result.failed',
    {
      on: {
        click: () => {
          ctrl.restartLevel();
          ctrl.redraw();
        },
      },
    },
    [
      h('h2', ctrl.trans.noarg('puzzleFailed')),
      h(
        'button',
        {
          on: {
            click: () => {
              ctrl.restartLevel();
              ctrl.redraw();
            },
          },
        },
        ctrl.trans.noarg('retry')
      ),
    ]
  );
}

function renderCompleted(ctrl: LearnCtrl) {
  const vm = ctrl.vm!;
  return h(
    'div.result.completed',
    {
      on: {
        click: () => {
          ctrl.nextLesson();
          ctrl.redraw();
        },
      },
    },
    [
      h('h2', ctrl.trans.noarg(congrats())),
      vm.level.nextButton || vm.stageState === 'end'
        ? h(
            'button',
            {
              on: {
                click: () => {
                  ctrl.completeLevel();
                  ctrl.redraw();
                },
              },
            },
            ctrl.trans.noarg('next')
          )
        : makeStars(vm.score || 0),
    ]
  );
}

function renderInfo(ctrl: LearnCtrl) {
  if (!ctrl.vm?.level.text) return null;
  return h(
    'a.info-text',
    {
      on: {
        click: e => {
          e.stopPropagation();
          ctrl.completeLevel();
        },
      },
    },
    ctrl.trans.noarg(ctrl.vm.level.text)
  );
}

function shogigroundBoard(ctrl: LearnCtrl): VNode {
  return h('div.sg-wrap', {
    hook: {
      insert: (vnode: VNode) => {
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
      },
      destroy: () => {
        ctrl.shogiground.detach({ board: true });
      },
    },
  });
}

export default function (ctrl: LearnCtrl): VNode {
  const vm = ctrl.vm!,
    stage = vm.stage,
    level = vm.level,
    stageStarting = vm.stageState === 'init',
    stageEnding = vm.stageState === 'completed';

  return h(
    'div.main.learn learn--run',
    {
      class: {
        starting: stageStarting,
        completed: stageEnding && !level.nextButton,
      },
    },
    [
      h('div.learn__side', side(ctrl)),
      h('div.learn__main.main-board', [
        stageStarting ? starting(ctrl) : null,
        stageEnding ? completed(ctrl) : null,
        shogigroundBoard(ctrl),
        renderInfo(ctrl),
      ]),
      h('div.learn__table', [
        h('div.wrap', [
          h('div.title', [
            h('div.stage-img.' + stage.key),
            h('div.text', [h('h2', ctrl.trans.noarg(stage.title)), h('p.subtitle', ctrl.trans.noarg(stage.subtitle))]),
          ]),
          vm.levelState === 'fail'
            ? renderFailed(ctrl)
            : vm.levelState === 'completed'
              ? renderCompleted(ctrl)
              : h('div.goal', typeof level.goal === 'string' ? ctrl.trans.noarg(level.goal) : level.goal(ctrl.trans)),
          progress(ctrl),
        ]),
      ]),
    ]
  );
}
