import { h } from 'snabbdom';
import { modal as menuModal } from 'board/menu';
import PuzzleController from '../ctrl';

export default function (ctrl: PuzzleController) {
  return menuModal(ctrl.trans, ctrl.redraw, ctrl.menu, menu => {
    return [
      h('section', [menu.flip(ctrl.trans.noarg('flipBoard'), ctrl.flipped(), ctrl.flip)]),
      h('section', [
        menu.zenMode(true),
        // menu.voiceInput(ctrl.voiceMoveEnabled, !spectator),
        // menu.keyboardInput(ctrl.keyboardMoveEnabled, !spectator),
      ]),
      h('section.board-menu__links', [
        h('a', { attrs: { target: '_blank', href: '/account/preferences/display' } }, 'Game display preferences'),
      ]),
    ];
  });
}
