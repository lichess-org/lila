import { h, VNodeData } from 'snabbdom';
import spinner from 'common/spinner';
import renderTabs from './tabs';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import LobbyController from '../ctrl';

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {};
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
      case 'pools':
        body = renderPools.render(ctrl);
        data = { hook: renderPools.hooks(ctrl) };
        break;
      case 'real_time':
        body = renderRealTime(ctrl);
        break;
      case 'seeks':
        body = renderSeeks(ctrl);
        break;
      case 'now_playing':
        body = renderPlaying(ctrl);
        break;
    }
  return h('div.lobby__app.lobby__app-' + ctrl.tab, [
    h('div.tabs-horiz', renderTabs(ctrl)),
    h('div.lobby__app__content.l' + (ctrl.redirecting ? 'redir' : ctrl.tab), data, body),
  ]);
}
