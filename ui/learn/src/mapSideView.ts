import { licon } from 'lib/licon';
import { a, bind, button, confirm, div, h1, h2, icon, img, span } from 'lib/view';

import type { LearnCtrl } from './ctrl';
import { BASE_LEARN_PATH, hashHref } from './hashRouting';
import { getStageRank } from './score';
import type { SideCtrl } from './sideCtrl';
import { categs } from './stage/list';
import { assetUrl } from './util';

export function mapSideView(ctrl: LearnCtrl) {
  if (ctrl.inStage()) return renderInStage(ctrl.sideCtrl);
  else return renderHome(ctrl.sideCtrl);
}

const helmImg = img(assetUrl + 'images/learn/brutal-helm.svg', '');

function renderInStage(ctrl: SideCtrl) {
  return div('.learn__side-map', [
    div('.stages', [
      a(BASE_LEARN_PATH)('.back', [helmImg(), i18n.site.menu]),
      ...categs.map((categ, categId) =>
        div('.categ', { class: { active: categId === ctrl.categId() } }, [
          h2({ hook: bind('click', () => ctrl.categId(categId)) }, [
            categ.name,
            icon(licon.GreaterThan)('.chevron'),
          ]),
          div(
            '.categ_stages',
            categ.stages.map(s => {
              const result = ctrl.data.stages[s.key];
              const scores = result?.scores ?? [];
              const completedCount = s.levels.filter(level => !!scores[level.id - 1]).length;
              const hasScore = completedCount > 0;
              const done = completedCount === s.levels.length;
              const classes = [
                'stage',
                s.id === ctrl.activeStageId() && 'active',
                hasScore ? 'ongoing' : 'future',
                done && 'done',
              ].filter(Boolean);
              const summary = done
                ? span('.stage__summary.stars', Array(4 - getStageRank(s, scores)).fill(icon(licon.Star)()))
                : hasScore
                  ? span('.stage__summary', `${completedCount} / ${s.levels.length}`)
                  : null;
              return a(hashHref(s.id))(`.${classes.join('.')}`, [
                img(s.image, 'Stage icon')(),
                span(s.title),
                summary,
              ]);
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
      helmImg('.decoration'),
      div('.learn__side-home__title', [h1(i18n.learn.learnChess), h2(i18n.learn.byPlaying)]),
    ]),
    div('.progress', [
      div('.text', i18n.learn.progressX(progress + '%')),
      div('.bar', { style: { width: progress + '%' } }),
    ]),
    progress > 0
      ? div(
          '.actions',
          button(
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
