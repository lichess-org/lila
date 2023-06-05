import { h } from 'snabbdom';
import RoundController from '../ctrl';
import { game as gameRoute } from 'game/router';
import { modal as menuModal } from 'board/menu';

export default function (ctrl: RoundController) {
  return menuModal(ctrl.trans, ctrl.redraw, ctrl.menu, menu => {
    const d = ctrl.data,
      spectator = d.player.spectator;
    return [
      h('section', [
        menu.flip(ctrl.noarg('flipBoard'), ctrl.flip, () => {
          if (d.tv) location.href = '/tv/' + d.tv.channel + (d.tv.flip ? '' : '?flip=1');
          else if (spectator) location.href = gameRoute(d, d.opponent.color);
          else ctrl.flipNow();
        }),
      ]),
      h('section', [
        menu.zenMode(!spectator),
        menu.voiceInput(ctrl.voiceMoveEnabled, !spectator),
        menu.keyboardInput(ctrl.keyboardMoveEnabled, !spectator),
      ]),
      h('section.board-menu__links', [
        h('a', { attrs: { target: '_blank', href: '/account/preferences/display' } }, 'Game display preferences'),
        h(
          'a',
          { attrs: { target: '_blank', href: '/account/preferences/game-behavior ' } },
          'Game behavior preferences'
        ),
      ]),
    ];
  });
}
