import { bind } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type LobbyController from '../../ctrl';
import { TimeMode } from '../../setup/util';

export function createSeek(ctrl: LobbyController): VNode {
  if (!ctrl.data.me)
    return h('div.create', h('a.button', { attrs: { href: '/signup' } }, i18n('signUp')));
  else
    return h(
      'div.create',
      h(
        'a.button.accent',
        {
          hook: bind('click', () => {
            ctrl.setupCtrl.open('hook', { timeMode: `${TimeMode.Corres}` });
            ctrl.redraw();
          }),
        },
        i18n('createAGame'),
      ),
    );
}
