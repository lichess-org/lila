import { VNode, h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import spinner from 'common/spinner';
import InsightCtrl from '../ctrl';
import { Result, Tab, tabs } from '../types';
import { outcomes } from './tabs/outcomes';
import { opponents } from './tabs/opponents';
import { moves } from './tabs/moves';
import { times } from './tabs/times';
import { analysis } from './tabs/analysis';
import { custom } from './tabs/custom';
import { filter } from './filter';

export default function (ctrl: InsightCtrl): VNode {
  return h('div.page-menu', [
    side(ctrl),
    h('div.page-menu__content box user-show', [tabsView(ctrl), content(ctrl, ctrl.activeTab)]),
  ]);
}

function side(ctrl: InsightCtrl): VNode {
  return h('aside.page-menu__menu', [
    h(
      'h2.title-username',
      { class: { small: ctrl.username.length > 11 } },
      h('a', { attrs: { href: `@/${ctrl.userId}` } }, ctrl.username)
    ),
    filter(ctrl),
  ]);
}

function tabsView(ctrl: InsightCtrl): VNode {
  return h(
    'div.angles number-menu number-menu--tabs menu-box-pop',
    tabs.map(tab =>
      h(
        `a.nm-item to-${tab}`,
        {
          class: {
            active: ctrl.activeTab === tab,
          },
          attrs: {
            'data-tab': tab,
          },
          hook: bind('click', () => ctrl.changeTab(tab), ctrl.redraw),
        },
        tab
      )
    )
  );
}

function content(ctrl: InsightCtrl, tab: Tab): VNode {
  const data = ctrl.data[tab] as any;
  if (!data)
    return h(
      'div.angle-content.loading',
      ctrl.isError
        ? h(
            'p',
            'Insights are not currently available. Sorry about that! If this keeps happening please report this on GitHub.'
          )
        : spinner()
    );

  const res =
    tab === 'outcomes'
      ? outcomes(ctrl, data)
      : tab === 'opponents'
        ? opponents(ctrl, data)
        : tab === 'moves'
          ? moves(ctrl, data)
          : tab === 'times'
            ? times(ctrl, data)
            : tab === 'analysis'
              ? analysis(ctrl, data)
              : custom(ctrl, data);
  return h('div.angle-content', [res, footer(data, ctrl.trans)]);
}

function footer(res: Result, trans: Trans): VNode {
  return h('div.footer', trans.plural('nbGames', res.nbOfGames));
}
