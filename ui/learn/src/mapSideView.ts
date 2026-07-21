import { a, bind, confirm, div, h1, h2, img, span } from 'lib/view';

import type { LearnCtrl } from './ctrl';
import { BASE_LEARN_PATH, hashHref } from './hashRouting';
import type { SideCtrl } from './sideCtrl';
import { categs } from './stage/list';
import { assetUrl } from './util';

export function mapSideView(ctrl: LearnCtrl) {
  if (ctrl.inStage()) return renderInStage(ctrl.sideCtrl);
  else return renderHome(ctrl.sideCtrl);
}

function renderInStage(ctrl: SideCtrl) {
  return div('.learn__side-map', [
    div('.stages', [
      a('.back', { href: BASE_LEARN_PATH }, [
        img({ src: assetUrl + 'images/learn/brutal-helm.svg' }),
        i18n.site.menu,
      ]),
      ...categs.map((categ, categId) =>
        div('.categ', { class: { active: categId === ctrl.categId() } }, [
          h2({ hook: bind('click', () => ctrl.categId(categId)) }, categ.name),
          div(
            '.categ_stages',
            categ.stages.map(s => {
              const result = ctrl.data.stages[s.key];
              const status = s.id === ctrl.activeStageId() ? 'active' : result ? 'done' : 'future';
              return a(`.stage.${status}`, { href: hashHref(s.id) }, [img({ src: s.image }), span(s.title)]);
            }),
          ),
        ]),
      ),
    ]),
  ]);
}

function renderHome(ctrl: SideCtrl) {
  const progress = ctrl.progress();
  return div('.learn__side-home', [
    div('.learn__side-home__header', [
      img('.decoration', { src: assetUrl + 'images/learn/brutal-helm.svg' }),
      div('.learn__side-home__title', [h1(i18n.learn.learnChess), h2(i18n.learn.byPlaying)]),
    ]),
    div('.progress', [
      div('.text', i18n.learn.progressX(progress + '%')),
      div('.bar', { style: { width: progress + '%' } }),
    ]),
    progress > 0
      ? div(
          '.actions',
          a(
            '.confirm',
            {
              hook: bind('click', async () => {
                if (await confirm(i18n.learn.youWillLoseAllYourProgress)) ctrl.reset();
              }),
            },
            i18n.learn.resetMyProgress,
          ),
        )
      : null,
  ]);
}
