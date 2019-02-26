import { h } from 'snabbdom';
import { VNodeData } from 'snabbdom/vnode';
import renderTabs from './tabs';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import { spinner } from './util';
import LobbyController from '../ctrl';

export default function(ctrl: LobbyController) {
  let body, data: VNodeData = {};
  if (ctrl.playban || ctrl.currentGame) return h('div#hooks_wrap');
  if (ctrl.redirecting) body = spinner();
  else switch (ctrl.tab) {
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
  return h('div#hooks_wrap', [
    h('div.lobby__app__tabs', renderTabs(ctrl)),
    h('div.lobby__app__content.l' + (ctrl.redirecting ? 'redir' : ctrl.tab), data, body)
  ]);
};
