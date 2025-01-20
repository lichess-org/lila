import { h } from 'snabbdom';
import { toggle } from 'common';
import { boardMenu as menuDropdown } from 'common/boardMenu';
import { boolPrefXhrToggle } from 'common/controls';
import type PuzzleCtrl from '../ctrl';

export default function (ctrl: PuzzleCtrl) {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => [
    h('section', [
      menu.flip(i18n.site.flipBoard, ctrl.flipped(), () => {
        ctrl.flip();
        ctrl.menu.toggle();
      }),
    ]),
    h('section', [
      menu.zenMode(true),
      menu.blindfold(
        toggle(ctrl.blindfold(), v => ctrl.blindfold(v)),
        true,
      ),
      menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), true),
      menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), true),
    ]),
    h('section.board-menu__links', [
      h('a', { attrs: { target: '_blank', href: '/account/preferences/display' } }, i18n.preferences.display),
    ]),
  ]);
}
