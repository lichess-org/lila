import { assetUrl } from './util';
import { categs } from './stage/list';
import type { SideCtrl } from './sideCtrl';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { BASE_LEARN_PATH, hashHref } from './hashRouting';
import type { LearnCtrl } from './ctrl';
import { confirm } from 'common/dialog';

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
        [h('img', { attrs: { src: assetUrl + 'images/learn/brutal-helm.svg' } }), i18n.site.menu],
      ),
      ...categs.map((categ, categId) =>
        h(
          'div.categ',
          {
            class: { active: categId === ctrl.categId() },
          },
          [
            h('h2', { hook: bind('click', () => ctrl.categId(categId)) }, categ.name),
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
                  [h('img', { attrs: { src: s.image } }), h('span', s.title)],
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
    h('h1', i18n.learn.learnChess),
    h('h2', i18n.learn.byPlaying),
    h('div.progress', [
      h('div.text', i18n.learn.progressX(progress + '%')),
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
              hook: bind('click', async () => {
                if (await confirm(i18n.learn.youWillLoseAllYourProgress)) ctrl.reset();
              }),
            },
            i18n.learn.resetMyProgress,
          )
        : null,
    ]),
  ]);
}
