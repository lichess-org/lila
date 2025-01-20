import { h } from 'snabbdom';
import type { LooseVNode } from 'common/snabbdom';
import type RoundController from '../ctrl';
import { boardMenu as menuDropdown } from 'common/boardMenu';
import { toggle } from 'common';
import { boolPrefXhrToggle } from 'common/controls';

export default function (ctrl: RoundController): LooseVNode {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => {
    const d = ctrl.data,
      spectator = d.player.spectator;
    return [
      h('section', [
        menu.flip(i18n.site.flipBoard, ctrl.flip, () => {
          ctrl.flipNow();
          ctrl.menu.toggle();
        }),
      ]),
      h('section', [
        menu.zenMode(true),
        menu.blindfold(
          toggle(ctrl.blindfold(), v => ctrl.blindfold(v)),
          !spectator,
        ),
        menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), !spectator),
        menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), !spectator),
        !spectator && (d.pref.submitMove || ctrl.voiceMove)
          ? menu.confirmMove(ctrl.confirmMoveToggle)
          : undefined,
      ]),
      h('section.board-menu__links', [
        h(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/display' } },
          i18n.preferences.display,
        ),
        h(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
          i18n.preferences.gameBehavior,
        ),
      ]),
    ];
  });
}
