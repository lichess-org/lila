import { h } from 'snabbdom';
import renderTabs from './tabs';
import renderPools from './pools';
import renderRealTime from './realTime/main';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import LobbyController from '../ctrl';

export default function(ctrl: LobbyController) {
  let body;
  if (ctrl.playban || ctrl.currentGame) return h('div#hooks_wrap');
  switch (ctrl.tab) {
    case 'pools':
      body = renderPools(ctrl);
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
    h('div.tabs', renderTabs(ctrl)),
    h('div.lobby_box.' + ctrl.tab, body)
  ]);
};
