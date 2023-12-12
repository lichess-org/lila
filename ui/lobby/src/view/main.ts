import { h, VNodeData, VNode } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import renderTabs from './tabs';
import * as xhr from '../xhr';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderCorrespondence from './correspondence';
import renderPlaying from './playing';
import LobbyController from '../ctrl';

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {},
    cls: string = ctrl.tab;
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
      case 'feed':
        body = h('div.daily-feed__updates', { hook: { insert: insertFeed, update: updateFeed } });
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

  function insertFeed(v: VNode) {
    if (ctrl.unreadFeedUpdates()) setTimeout(ctrl.redraw);
    ctrl.unreadFeedUpdates(false);
    (v.elm as HTMLElement).innerHTML = ctrl.feedHtml;
  }

  async function updateFeed(_: VNode, v: VNode) {
    if (!ctrl.unreadFeedUpdates()) return;
    ctrl.feedHtml = await xhr.feed();
    insertFeed(v);
  }
}
