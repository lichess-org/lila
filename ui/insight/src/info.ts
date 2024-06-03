import * as licon from 'common/licon';
import { onInsert } from 'common/snabbdom';
import { numberFormat } from 'common/number';
import { userLink } from 'common/userLink';
import { h } from 'snabbdom';
import Ctrl from './ctrl';
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
                  rel: 'noopener',
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
                    hook: onInsert(el => el.insertAdjacentHTML('afterbegin', site.spinnerHtml)),
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
