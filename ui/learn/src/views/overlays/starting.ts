import type { MaybeVNode } from 'common/snabbdom';
import { i18n, i18nFormat } from 'i18n';
import { h } from 'snabbdom';
import type LearnCtrl from '../../ctrl';

export default function (ctrl: LearnCtrl): MaybeVNode {
  if (!ctrl.vm) return null;
  else
    return h(
      'div.learn__screen-overlay.starting',
      {
        on: {
          click: () => {
            if (ctrl.vm) ctrl.vm.stageState = 'running';
            ctrl.redraw();
          },
        },
      },
      h('div.learn__screen.', [
        h('h1', `${i18nFormat('learn:stageX', ctrl.vm.stage.id)}: ${ctrl.vm.stage.title}`),
        h(`div.stage-img.${ctrl.vm.stage.key}`),
        h('p', ctrl.vm.stage.intro),
        h('div.buttons', h('a.next', i18n('learn:letsGo'))),
      ]),
    );
}
