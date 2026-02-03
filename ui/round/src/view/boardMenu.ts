import { hl, type LooseVNode, boardMenu as menuDropdown, cmnToggleWrap, boolPrefXhrToggle } from 'lib/view';
import type RoundController from '../ctrl';
import { toggle } from 'lib';
import { displayColumns, isTouchDevice } from 'lib/device';
import { storage } from 'lib/storage';

export default function (ctrl: RoundController): LooseVNode {
  return menuDropdown(ctrl.redraw, ctrl.menu, menu => {
    const d = ctrl.data,
      spectator = d.player.spectator,
      portraitMobile = displayColumns() === 1 && isTouchDevice(),
      swapClockStorage = storage.boolean('swapClock');
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
          cmnToggleWrap({
            id: 'haptics',
            name: 'Vibration feedback',
            checked: ctrl.vibration(),
            change: v => ctrl.vibration(v),
            redraw: ctrl.redraw,
          }),
        portraitMobile &&
          cmnToggleWrap({
            id: 'swapClock',
            name: 'Show clock on left',
            checked: swapClockStorage.get(),
            change: v => swapClockStorage.set(v),
            redraw: ctrl.redraw,
          }),

        menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), !spectator),
        !portraitMobile &&
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
