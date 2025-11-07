import * as licon from 'lib/licon';
import { onInsert, spinnerHtml } from 'lib/view';
import { numberFormat } from 'lib/i18n';
import { userLink } from 'lib/view/userLink';
import { h } from 'snabbdom';
import type Ctrl from './ctrl';
import { registerFormHandler } from './insight';

const shareStates = ['nobody', 'friends only', 'everybody'];

export default function (ctrl: Ctrl) {
  const shareText = 'Shared with ' + shareStates[ctrl.user.shareId] + '.';
  return h('div.info.box', [
    h('div.top', userLink(ctrl.user)),
    h('div.content', [
      h('p', ['Insights over ', h('strong', numberFormat(ctrl.user.nbGames)), ' rated games.']),
      h(
        'p.share',
        ctrl.own
          ? h(
              'a',
              {
                attrs: {
                  href: '/account/preferences/privacy#shareYourInsightsData',
                  target: '_blank',
                },
              },
              shareText,
            )
          : shareText,
      ),
    ]),
    h(
      'div.refresh',
      ctrl.env.user.stale
        ? h('div.insight-stale', [
            h('p', 'There are new games to learn from!'),
            h(
              'form.insight-refresh',
              {
                attrs: {
                  action: `/insights/refresh/${ctrl.env.user.id}`,
                  method: 'post',
                },
                hook: onInsert(_el => registerFormHandler()),
              },
              [
                h('button.button.text', { attrs: { 'data-icon': licon.Checkmark } }, 'Update insights'),
                h(
                  'div.crunching.none',
                  {
                    hook: onInsert(el => el.insertAdjacentHTML('afterbegin', spinnerHtml)),
                  },
                  [h('br'), h('p', h('strong', 'Now crunching data just for you!'))],
                ),
              ],
            ),
          ])
        : null,
    ),
  ]);
}
