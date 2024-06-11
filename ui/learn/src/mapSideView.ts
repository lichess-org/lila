import * as util from './util';
import * as stages from './stage/list';
import { SideCtrl } from './sideCtrl';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { BASE_LEARN_PATH, hashHref } from './hashRouting';
import { LearnCtrl } from './ctrl';

export function mapSideView(ctrl: LearnCtrl) {
  if (ctrl.inStage()) return renderInStage(ctrl.sideCtrl);
  else return renderHome(ctrl.sideCtrl);
}

function renderInStage(ctrl: SideCtrl) {
  return h('div.learn__side-map', [
    h('div.stages', [
      h(
        'a.back',
        {
          attrs: { href: BASE_LEARN_PATH },
        },
        [
          h('img', { attrs: { src: util.assetUrl + 'images/learn/brutal-helm.svg' } }),
          ctrl.trans.noarg('menu'),
        ],
      ),
      ...stages.categs.map((categ, categId) =>
        h(
          'div.categ',
          {
            class: { active: categId == ctrl.categId() },
          },
          [
            h('h2', { hook: bind('click', () => ctrl.categId(categId)) }, ctrl.trans.noarg(categ.name)),
            h(
              'div.categ_stages',
              categ.stages.map(s => {
                const result = ctrl.data.stages[s.key];
                const status = s.id === ctrl.activeStageId() ? 'active' : result ? 'done' : 'future';
                return h(
                  `a.stage.${status}`,
                  {
                    attrs: { href: hashHref(s.id) },
                  },
                  [h('img', { attrs: { src: s.image } }), h('span', ctrl.trans.noarg(s.title))],
                );
              }),
            ),
          ],
        ),
      ),
    ]),
  ]);
}

function renderHome(ctrl: SideCtrl) {
  const progress = ctrl.progress();
  return h('div.learn__side-home', [
    h('i.fat'),
    h('h1', ctrl.trans.noarg('learnChess')),
    h('h2', ctrl.trans.noarg('byPlaying')),
    h('div.progress', [
      h('div.text', ctrl.trans('progressX', progress + '%')),
      h('div.bar', {
        style: {
          width: progress + '%',
        },
      }),
    ]),
    h('div.actions', [
      progress > 0
        ? h(
            'a.confirm',
            {
              hook: bind(
                'click',
                () => confirm(ctrl.trans.noarg('youWillLoseAllYourProgress')) && ctrl.reset(),
              ),
            },
            ctrl.trans.noarg('resetMyProgress'),
          )
        : null,
    ]),
  ]);
}
