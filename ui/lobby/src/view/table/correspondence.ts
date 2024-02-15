import { bind } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import LobbyController from '../../ctrl';

export function createSeek(ctrl: LobbyController): VNode {
  if (!ctrl.data.me) return h('div.create', h('a.button', { attrs: { href: '/signup' } }, ctrl.trans.noarg('signUp')));
  else
    return h(
      'div.create',
      h(
        'a.button.accent',
        {
          hook: bind('click', () => {
            $('.lobby__start .config_hook')
              .each(function (this: HTMLElement) {
                this.dataset.hrefAddon = '?time=correspondence';
              })
              .trigger('mousedown')
              .trigger('click');
          }),
        },
        ctrl.trans('createAGame')
      )
    );
}
