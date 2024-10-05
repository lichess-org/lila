import { looseH as h, bind, VNode, LooseVNodes } from 'common/snabbdom';
import * as licon from 'common/licon';
import { renderHookList } from './hookList';
import { renderHookGraph } from './hookGraph';
import { renderHookFilter } from './hookFilter';
import { perfNames } from '../../constants';
import { Seek } from '../../interfaces';
import perfIcons from 'common/perfIcons';
import LobbyController from '../../ctrl';

export function renderLobbyTab(ctrl: LobbyController): { body: LooseVNodes; cls: string } {
  return {
    body: ctrl.tab.active === 'realtime' ? renderRealtime(ctrl) : renderCorrespondence(ctrl),
    cls: ctrl.tab.active,
  };
}

function renderRealtime(ctrl: LobbyController): LooseVNodes {
  const { visible, hidden } = ctrl.filter.filter(ctrl.mode === 'graph' ? ctrl.data.hooks : ctrl.stepHooks);
  const otherMode = ctrl.mode === 'graph' ? 'list' : 'graph';
  return [
    h('i.toggle.toggle-filter', {
      class: { gamesFiltered: hidden > 0, active: ctrl.filter.open },
      hook: bind('mousedown', ctrl.filter.toggle, ctrl.redraw),
      attrs: { 'data-icon': ctrl.filter.open ? licon.X : licon.Gear, title: ctrl.trans.noarg('filterGames') },
    }),
    !ctrl.filter.open &&
      h('i.toggle', {
        key: `set-mode-${otherMode}`,
        attrs: {
          title: ctrl.trans.noarg(otherMode),
          'data-icon': otherMode === 'graph' ? licon.LineGraph : licon.List,
        },
        hook: bind('mousedown', _ => ctrl.setMode(otherMode), ctrl.redraw),
      }),
    ctrl.filter.open
      ? renderHookFilter(ctrl)
      : ctrl.mode === 'graph'
        ? renderHookGraph(ctrl, visible)
        : renderHookList(ctrl, visible),
  ];
}

function renderCorrespondence(ctrl: LobbyController): LooseVNodes {
  return [
    h('table.hooks__list', [
      h(
        'thead',
        h(
          'tr',
          ['player', 'rating', 'time', 'mode'].map(header => h('th', ctrl.trans(header))),
        ),
      ),
      h(
        'tbody',
        {
          hook: bind('click', e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') {
                if (!ctrl.me) {
                  if (confirm(ctrl.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
                  return;
                }
                return ctrl.clickSeek(el.dataset['id']!);
              }
            } while (el.nodeName !== 'TABLE');
          }),
        },
        ctrl.data.seeks.map(s => renderSeek(ctrl, s)),
      ),
    ]),
    createSeek(ctrl),
  ];
}

function renderSeek(ctrl: LobbyController, seek: Seek): VNode {
  const klass = seek.action === 'joinSeek' ? 'join' : 'cancel',
    noarg = ctrl.trans.noarg;
  return h(
    'tr.seek.' + klass,
    {
      key: seek.id,
      attrs: {
        role: 'button',
        title:
          seek.action === 'joinSeek'
            ? noarg('joinTheGame') + ' - ' + perfNames[seek.perf.key]
            : noarg('cancel'),
        'data-id': seek.id,
      },
    },
    [
      h(
        'td',
        seek.rating
          ? h('span.ulpt', { attrs: { 'data-href': '/@/' + seek.username } }, seek.username)
          : 'Anonymous',
      ),
      h('td', seek.rating && ctrl.opts.showRatings ? seek.rating + (seek.provisional ? '?' : '') : ''),
      h('td', seek.days ? ctrl.trans.pluralSame('nbDays', seek.days) : '∞'),
      h(
        'td',
        h('span', [
          h('span.varicon', { attrs: { 'data-icon': perfIcons[seek.perf.key] } }),
          noarg(seek.mode === 1 ? 'rated' : 'casual'),
        ]),
      ),
    ],
  );
}

function createSeek(ctrl: LobbyController): VNode | undefined {
  if (ctrl.me && ctrl.data.seeks.length < 8)
    return h('div.create', [
      h(
        'a.button',
        {
          hook: bind(
            'click',
            () => ctrl.setupCtrl.openModal('hook', { variant: 'standard', timeMode: 'correspondence' }),
            ctrl.redraw,
          ),
        },
        ctrl.trans('createAGame'),
      ),
    ]);
  return;
}
