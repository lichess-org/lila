import { h, VNodeData, VNode } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { storedIntProp } from 'common/storage';
import renderTabs from './tabs';
import * as xhr from '../xhr';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderCorrespondence from './correspondence';
import renderPlaying from './playing';
import LobbyController from '../ctrl';

function updateFeed(v: VNode) {
  xhr.feed().then(html => ((v.elm as HTMLElement).innerHTML = html));
}

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {},
    cls: string = ctrl.tab;
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
      case 'feed':
        storedIntProp('feed.lastUpdate', 0)(ctrl.opts.lastFeedRev);
        body = h('div', { hook: { insert: updateFeed, update: (_, v: VNode) => updateFeed(v) } });
        break;
      case 'pools':
        body = renderPools.render(ctrl);
        data = { hook: renderPools.hooks(ctrl) };
        break;
      case 'custom_games':
        body = ctrl.customGameTab === 'real_time' ? renderRealTime(ctrl) : renderCorrespondence(ctrl);
        cls = ctrl.customGameTab;
        break;
      case 'now_playing':
        body = renderPlaying(ctrl);
        break;
    }
  return h('div.lobby__app.lobby__app-' + ctrl.tab, [
    ...renderTabs(ctrl),
    h('div.lobby__app__content.l' + (ctrl.redirecting ? 'redir' : cls), data, body),
  ]);
}
