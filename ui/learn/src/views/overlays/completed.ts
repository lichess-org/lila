import { VNode, h } from 'snabbdom';
import { nextStage } from '../../categories';
import LearnCtrl from '../../ctrl';
import { average } from '../../util';

function makeStars(score: number): VNode[] {
  const stars = [];
  for (let i = 0; i < score; i++) stars.push(h('div.star-wrap', h('i.star')));
  return stars;
}

export default function (ctrl: LearnCtrl) {
  if (!ctrl.vm) return;
  const stage = ctrl.vm.stage,
    next = nextStage(stage.id),
    stars = Math.floor(average(ctrl.progress.get(stage.key)));
  return h(
    'div.learn__screen-overlay.completed',
    {
      on: {
        click: () => {
          if (ctrl.vm) ctrl.vm.stageState = 'end';
          ctrl.redraw();
        },
      },
    },
    h('div.learn__screen', [
      h('div.stars', makeStars(stars)),
      h('h1', ctrl.trans('stageXComplete', stage.id)),
      h('p', ctrl.trans.noarg(stage.complete)),
      h('div.buttons', [
        next
          ? h(
              'a.next',
              {
                on: {
                  click: () => {
                    ctrl.nextLesson();
                    ctrl.redraw();
                  },
                },
              },
              [
                ctrl.trans.noarg('next') + ': ',
                ctrl.trans.noarg(next.title) + ' ',
                h('i', { attrs: { 'data-icon': 'H' } }),
              ]
            )
          : null,
        h(
          'a.back.text',
          {
            dataset: {
              icon: 'I',
            },
            on: {
              click: () => {
                ctrl.setHome();
                ctrl.redraw();
              },
            },
          },
          ctrl.trans.noarg('backToMenu')
        ),
      ]),
    ])
  );
}
