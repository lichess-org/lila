import spinner from 'common/spinner';
import { VNodeData, h } from 'snabbdom';
import LobbyController from '../ctrl';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import renderRealTime from './realTime/main';
import renderTabs from './tabs';

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {};
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
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
