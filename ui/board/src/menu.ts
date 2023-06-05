import { h } from 'snabbdom';
import { ToggleWithUsed } from 'common/storage';
import * as licon from 'common/licon';

export function toggleButton(toggle: ToggleWithUsed, title: string) {
  return h(
    'button.fbt.board-menu-toggle',
    {
      class: { active: toggle() },
      attrs: {
        title,
        'data-act': 'menu',
        'data-icon': licon.Hamburger,
      },
    },
    toggle.used() ? undefined : h('div.board-menu-toggle__new')
  );
}
