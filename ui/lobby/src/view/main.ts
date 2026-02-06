import { h, type VNodeData } from 'snabbdom';
import { spinnerVdom as spinner } from 'lib/view';
import renderTabs from './tabs';
import * as renderPools from './pools';
import renderRealTime from './realTime/main';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import type LobbyController from '../ctrl';

export default function (ctrl: LobbyController) {
  let body,
    data: VNodeData = {};
  const redirBlock = ctrl.redirecting && ctrl.tab !== 'pools';
  if (redirBlock) body = spinner();
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
  const contentKey = ctrl.tab === 'real_time' ? `${ctrl.tab}-${ctrl.mode}` : ctrl.tab;
  return h(`div.lobby__app.lobby__app-${ctrl.tab}.lck-${contentKey}`, [
    h('div.tabs-horiz', { attrs: { role: 'tablist' } }, renderTabs(ctrl)),
    h(`div.lobby__app__content.l${redirBlock ? 'redir' : ctrl.tab}`, { key: contentKey, ...data }, body),
  ]);
}
