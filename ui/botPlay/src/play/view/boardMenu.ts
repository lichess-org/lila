import { h } from 'snabbdom';
import type { LooseVNode } from 'common/snabbdom';
import type PlayCtrl from '../playCtrl';
import { boardMenu as menuDropdown } from 'common/boardMenu';
// import { toggle } from 'common';
// import { boolPrefXhrToggle } from 'common/controls';

export default function (ctrl: PlayCtrl): LooseVNode {
  return menuDropdown(ctrl.opts.redraw, ctrl.menu, menu => [
    h('section', [
      menu.flip(i18n.site.flipBoard, ctrl.flipped(), () => {
        ctrl.flip();
        ctrl.menu.toggle();
      }),
    ]),
    h('section', [
      menu.zenMode(true),
      menu.blindfold(ctrl.blindfold),
      // menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), !spectator),
      // menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), !spectator),
      // !spectator && (d.pref.submitMove || ctrl.voiceMove)
      //   ? menu.confirmMove(ctrl.confirmMoveToggle)
      //   : undefined,
    ]),
    h('section.board-menu__links', [
      h('a', { attrs: { target: '_blank', href: '/account/preferences/display' } }, i18n.preferences.display),
      h(
        'a',
        { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
        i18n.preferences.gameBehavior,
      ),
    ]),
  ]);
}
