import { type VNodeData } from 'snabbdom';

import { spinnerVdom as spinner, hl } from 'lib/view';

import type LobbyController from '../ctrl';
import renderSeeks from './correspondence';
import renderPlaying from './playing';
import renderRealTime from './realTime/main';
import { render as renderShortcuts, hooks } from './shortcuts';
import renderTabs from './tabs';

export default function (ctrl: LobbyController) {
  let body;
  let data: VNodeData = {};
  const redirBlock = ctrl.redirecting && ctrl.tab !== 'shortcuts';
  if (redirBlock) body = spinner();
  else
    switch (ctrl.tab) {
      case 'shortcuts':
        body = renderShortcuts(ctrl);
        data = { hook: hooks(ctrl) };
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
  return hl(`div.lobby__app.lobby__app-${ctrl.tab}.lck-${contentKey}`, [
    hl('div.tabs-horiz', { attrs: { role: 'tablist' } }, renderTabs(ctrl)),
    hl(`div.lobby__app__content.${redirBlock ? 'redir' : ctrl.tab}`, data, body),
  ]);
}
