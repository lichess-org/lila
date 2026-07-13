import { confirm, html } from 'lib/view';

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
  return html`
    <div class="learn__side-map">
      <div class="stages">
        <a class="back" href=${BASE_LEARN_PATH}>
          <img src="${assetUrl}images/learn/brutal-helm.svg" alt="" />
          ${i18n.site.menu}
        </a>
        ${categs.map(
          (categ, categId) => html`
            <div class=${{ categ: true, active: categId === ctrl.categId() }}>
              <h2 @on:click=${() => ctrl.categId(categId)}>${categ.name}</h2>
              <div class="categ_stages">
                ${categ.stages.map(({ key, id, image, title }) => {
                  const result = ctrl.data.stages[key];
                  const status = id === ctrl.activeStageId() ? 'active' : result ? 'done' : 'future';
                  return html`
                    <a class="stage ${status}" href=${hashHref(id)}>
                      <img src=${image} alt="" />
                      <span>${title}</span>
                    </a>
                  `;
                })}
              </div>
            </div>
          `,
        )}
      </div>
    </div>
  `;
}

function renderHome(ctrl: SideCtrl) {
  const progress = ctrl.progress();

  return html`
    <div class="learn__side-home">
      <div class="learn__side-home__header">
        <img class="decoration" src="${assetUrl}images/learn/brutal-helm.svg" alt="" />
        <div class="learn__side-home__title">
          <h1>${i18n.learn.learnChess}</h1>
          <h2>${i18n.learn.byPlaying}</h2>
        </div>
      </div>
      <div class="progress">
        <div class="text">${i18n.learn.progressX(progress + '%')}</div>
        <div class="bar" style="width: ${progress}%" />
      </div>
      ${progress > 0
        ? html`<div class="actions">
            <a
              class="confirm"
              @on:click=${async () => {
                if (await confirm(i18n.learn.youWillLoseAllYourProgress)) ctrl.reset();
              }}
              >${i18n.learn.resetMyProgress}</a
            >
          </div>`
        : null}
    </div>
  `;
}
