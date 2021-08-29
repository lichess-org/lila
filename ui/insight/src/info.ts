import { onInsert } from 'common/snabbdom';
import { h } from 'snabbdom';
import Ctrl from './ctrl';

const shareStates = ['nobody', 'friends only', 'everybody'];

export default function (ctrl: Ctrl) {
  const shareText = 'Shared with ' + shareStates[ctrl.user.shareId] + '.';
  return h('div.info.box', [
    h('div.top', [h('a.username.user-link.insight-ulpt', { attrs: { href: '/@/' + ctrl.user.name } }, ctrl.user.name)]),
    h('div.content', [
      h('p', ['Insights over ', h('strong', ctrl.user.nbGames), ' rated games.']),
      h(
        'p.share',
        ctrl.own
          ? h(
              'a',
              {
                attrs: {
                  href: '/account/preferences/privacy',
                  target: '_blank',
                  rel: 'noopener',
                },
              },
              shareText
            )
          : shareText
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
                hook: onInsert(_el => lichess.refreshInsightForm()),
              },
              [
                h('button.button.text', { attrs: { 'data-icon': '' } }, 'Update insights'),
                h(
                  'div.crunching.none',
                  {
                    hook: onInsert(el => el.insertAdjacentHTML('afterbegin', lichess.spinnerHtml)),
                  },
                  [h('br'), h('p', h('strong', 'Now crunching data just for you!'))]
                ),
              ]
            ),
          ])
        : null
    ),
  ]);
}
