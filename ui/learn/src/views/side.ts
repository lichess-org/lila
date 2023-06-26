import { VNode, h } from 'snabbdom';
import LearnCtrl from '../ctrl';

export default function (ctrl: LearnCtrl): VNode {
  return h('div.learn__side-map', [
    h('div.stages', [
      h(
        'a.back',
        {
          on: {
            click: () => {
              ctrl.setHome();
              ctrl.redraw();
            },
          },
        },
        [h('div.stage-img.samurai-helmet'), ctrl.trans.noarg('menu')]
      ),
      ...ctrl.categories.map(categ => {
        return h(
          'div.categ',
          {
            class: { active: categ.key === ctrl.vm?.sideCategory },
          },
          [
            h(
              'h2',
              {
                on: {
                  click: () => {
                    if (ctrl.vm) {
                      ctrl.vm.sideCategory = categ.key;
                      ctrl.redraw();
                    }
                  },
                },
              },
              ctrl.trans.noarg(categ.key)
            ),
            h(
              'div.categ_stages',
              categ.stages.map(s => {
                const result = ctrl.progress.get(s.key).filter(s => s > 0).length === s.levels.length;
                const status = s.key === ctrl.vm?.stage.key ? 'active' : result ? 'done' : 'future';
                return h(
                  'a.stage.' + status,
                  {
                    on: {
                      click: () => {
                        ctrl.setLesson(s.id);
                        ctrl.redraw();
                      },
                    },
                  },
                  [h(`div.stage-img.${s.key}`), h('span', ctrl.trans.noarg(s.title))]
                );
              })
            ),
          ]
        );
      }),
    ]),
  ]);
}
