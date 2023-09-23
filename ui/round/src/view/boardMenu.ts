import { h } from 'snabbdom';
import RoundController from '../ctrl';
import { menu as menuDropdown } from 'board/menu';
import { boolPrefXhrToggle } from 'common/controls';

export default function (ctrl: RoundController) {
  return menuDropdown(ctrl.trans, ctrl.redraw, ctrl.menu, menu => {
    const d = ctrl.data,
      spectator = d.player.spectator;
    return [
      h('section', [menu.flip(ctrl.noarg('flipBoard'), ctrl.flip, ctrl.flipNow)]),
      h('section', [
        menu.zenMode(true),
        menu.voiceInput(boolPrefXhrToggle('voice', !!ctrl.voiceMove), !spectator),
        menu.keyboardInput(boolPrefXhrToggle('keyboardMove', !!ctrl.keyboardMove), !spectator),
        !spectator && d.pref.submitMove ? menu.confirmMove(ctrl.confirmMoveEnabled) : undefined,
      ]),
      h('section.board-menu__links', [
        h(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/display' } },
          'Game display preferences',
        ),
        h(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
          'Game behavior preferences',
        ),
      ]),
    ];
  });
}
