import { looseH as h, bind, LooseVNodes } from 'common/snabbdom';
import { VNodeData } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { renderQuickTab } from './quickTab';
import { renderLobbyTab } from './lobby/lobbyTab';
import { renderYourGamesTab } from './yourGamesTab';
import LobbyController from '../ctrl';
import { Tab } from '../interfaces';

export function renderApp(ctrl: LobbyController) {
  let body: LooseVNodes = [],
    data: VNodeData = {},
    cls: string = ctrl.tab.appTab;
  const redirBlock = ctrl.redirecting && !ctrl.tab.showingHooks;
  if (redirBlock) body = [spinner()];
  else
    switch (ctrl.tab.appTab) {
      case 'quick':
        ({ body, data } = renderQuickTab(ctrl));
        break;
      case 'lobby':
        ({ body, cls } = renderLobbyTab(ctrl));
        break;
      case 'tournament':
        data = tournamentTabData();
        break;
      case 'your':
        body = renderYourGamesTab(ctrl);
        break;
    }
  const secondaries = ctrl.tab.visibleSecondaryTabNames;
  return h('div.lobby__app.lobby__app-' + ctrl.tab.appTab, [
    h(
      'div.tabs-horiz',
      { attrs: { role: 'tablist' } },
      ctrl.tab.visibleAppTabNames.map(([k, v]) => renderTab(ctrl, k, v)),
    ),
    secondaries.length > 1 &&
      h(
        'div.tabs-horiz.secondary-tabs',
        { attrs: { role: 'tablist' } },
        secondaries.map(([k, v]) => renderTab(ctrl, k, v)),
      ),
    h('div.lobby__app__content.' + (redirBlock ? 'redir' : cls), data, body),
  ]);
}

function renderTab(ctrl: LobbyController, key: Tab, name: string) {
  const myTurnPovsNb = ctrl.data.nowPlaying.filter(p => p.isMyTurn).length;
  return h(
    'span',
    {
      attrs: { role: 'tab' },
      class: {
        active: ctrl.tab.isShowing(key),
        glowing: ctrl.tab.active !== 'quick' && key === 'quick' && !!ctrl.poolMember,
      },
      hook: bind('mousedown', _ => ctrl.setTab(key), ctrl.redraw),
    },
    [
      name,
      key === 'your' && myTurnPovsNb > 0 && h('i.unread', myTurnPovsNb >= 99 ? '99+' : String(myTurnPovsNb)),
    ],
  );
}

function tournamentTabData(): VNodeData {
  // should xhr this, but this hack will work for now
  return {
    hook: {
      create: (_, vnode) => {
        const events = document.querySelector('.lobby__tournaments-simuls') as HTMLElement;
        if (!events) return;
        events
          .querySelectorAll<HTMLElement>('tr[data-href]')
          .forEach(row => row.addEventListener('click', () => (location.href = row.dataset.href!)));
        if (!events) return;
        events.style.display = 'block';
        vnode.elm?.appendChild(events);
      },
      destroy: vnode => {
        const events = vnode.elm?.firstChild as HTMLElement;
        if (!events) return;
        events.style.display = 'none';
        document.body.appendChild(events);
      },
    },
  };
}
