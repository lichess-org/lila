import * as util from '../util';
import * as stages from '../stage/list';
import { SnabbdomSideCtrl } from './sideCtrl';
import { h } from 'snabbdom';

export function mapSideView(ctrl: SnabbdomSideCtrl) {
  if (ctrl.inStage()) return renderInStage(ctrl);
  else return renderHome(ctrl);
}

function renderInStage(ctrl: SnabbdomSideCtrl) {
  return h('div.learn__side-map', [
    h('div.stages', [
      h(
        'a.back',
        {
          attrs: { href: '/' },
          // TODO:
          // config: m.route,
        },
        [
          h('img', { attrs: { src: util.assetUrl + 'images/learn/brutal-helm.svg' } }),
          ctrl.trans.noarg('menu'),
        ],
      ),
      ...stages.categs.map(function (categ, categId) {
        return h(
          'div.categ',
          {
            class: { active: categId == ctrl.categId() },
          },
          [
            h(
              'h2',
              {
                onclick: function () {
                  // TODO:
                  ctrl.categId(categId);
                },
              },
              ctrl.trans.noarg(categ.name),
            ),
            h(
              'div.categ_stages',
              categ.stages.map(function (s) {
                const result = ctrl.data.stages[s.key];
                const status = s.id === ctrl.active() ? 'active' : result ? 'done' : 'future';
                return h(
                  `a.stage.${status}`,
                  {
                    attrs: { href: '/' + s.id },
                    // TODO:
                    // config: m.route,
                  },
                  [h('img', { attrs: { src: s.image } }), h('span', ctrl.trans.noarg(s.title))],
                );
              }),
            ),
          ],
        );
      }),
    ]),
  ]);
}

function renderHome(ctrl: SnabbdomSideCtrl) {
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
              onclick: function () {
                // TODO:
                if (confirm(ctrl.trans.noarg('youWillLoseAllYourProgress'))) ctrl.reset();
              },
            },
            ctrl.trans.noarg('resetMyProgress'),
          )
        : null,
    ]),
  ]);
}
