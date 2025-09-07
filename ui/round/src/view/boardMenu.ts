import { hl, type LooseVNode } from 'lib/snabbdom';
import type RoundController from '../ctrl';
import { boardMenu as menuDropdown } from 'lib/view/boardMenu';
import { toggle } from 'lib';
import { toggle as cmnToggle, boolPrefXhrToggle } from 'lib/view/controls';

export default function (ctrl: RoundController): LooseVNode {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => {
    const d = ctrl.data,
      spectator = d.player.spectator;
    return [
      hl('section', [
        menu.flip(i18n.site.flipBoard, ctrl.flip, () => {
          ctrl.flipNow();
          ctrl.menu.toggle();
        }),
      ]),
      hl('section', [
        menu.zenMode(true),
        menu.blindfold(
          toggle(ctrl.blindfold(), v => ctrl.blindfold(v)),
          !spectator,
        ),
        'vibrate' in navigator &&
          cmnToggle(
            {
              name: 'Vibration feedback',
              id: 'haptics',
              checked: ctrl.vibration(),
              change: v => ctrl.vibration(v),
            },
            ctrl.redraw,
          ),
        menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), !spectator),
        menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), !spectator),
        !spectator && (d.pref.submitMove || ctrl.voiceMove)
          ? menu.confirmMove(ctrl.confirmMoveToggle)
          : undefined,
      ]),
      hl('section.board-menu__links', [
        hl(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/display' } },
          i18n.preferences.display,
        ),
        hl(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
          i18n.preferences.gameBehavior,
        ),
      ]),
    ];
  });
}
