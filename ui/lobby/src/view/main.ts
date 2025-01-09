import spinner from 'common/spinner';
import { VNode, VNodeData, h } from 'snabbdom';
import LobbyController from '../ctrl';
import renderPlaying from './playing';
import renderTable from './table/main';
import * as renderPresets from './presets';
import renderTabs from './tabs';
import { setupModal } from '../setup/view';
import { dataIcon } from 'common/snabbdom';

export default function (ctrl: LobbyController): VNode {
  let body,
    data: VNodeData = {};
  if (ctrl.redirecting) body = spinner();
  else
    switch (ctrl.tab) {
      case 'presets':
        body = renderPresets.render(ctrl);
        data = { hook: renderPresets.presetHooks(ctrl) };
        break;
      case 'real_time':
      case 'seeks':
        body = renderTable(ctrl);
        data = { attrs: dataIcon('󰀀') };
        break;
      case 'now_playing':
        body = renderPlaying(ctrl);
        break;
    }
  return h('div.lobby__app.lobby__app-' + ctrl.tab, [
    h('div.tabs-horiz', renderTabs(ctrl)),
    h('div.lobby__app__content.l' + (ctrl.redirecting ? 'redir' : ctrl.tab), data, body),
    ctrl.setupCtrl.isOpen ? setupModal(ctrl.setupCtrl) : undefined,
  ]);
}
