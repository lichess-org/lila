import { h } from 'snabbdom';
import { ToggleWithUsed, toggleWithUsed } from 'common/storage';
import { Toggle, toggle as baseToggle } from 'common/common';
import * as licon from 'common/licon';
import { MaybeVNode, MaybeVNodes, bind, dataIcon } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { Redraw } from 'chessground/types';
import * as controls from 'common/controls';

export const toggle = (redraw: () => void): ToggleWithUsed =>
  toggleWithUsed('boardMenuToggleUsed', baseToggle(false, redraw));

export const toggleButton = (toggle: ToggleWithUsed, title: string) =>
  h(
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

export const modal = (
  trans: Trans,
  redraw: Redraw,
  toggle: Toggle,
  content: (menu: BoardMenu) => MaybeVNodes
): MaybeVNode =>
  toggle()
    ? snabModal({
        class: 'board-menu',
        onClose: () => toggle(false),
        content: content(new BoardMenu(trans, redraw)),
      })
    : undefined;

export class BoardMenu {
  constructor(readonly trans: Trans, readonly redraw: Redraw) {}

  flip = (name: string, active: boolean, onChange: () => void) =>
    h(
      'button.button.text',
      {
        class: { active },
        attrs: {
          title: 'Hotkey: f',
          ...dataIcon(licon.ChasingArrows),
        },
        hook: bind('click', onChange),
      },
      name
    );

  zenMode = (enabled = true) =>
    this.cmnToggle({
      name: 'Zen mode',
      id: 'zen',
      checked: $('body').hasClass('zen'),
      change: () => lichess.pubsub.emit('zen'),
      disabled: !enabled,
    });

  voiceInput = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Voice input',
      id: 'voice',
      checked: toggle(),
      change: toggle,
      cls: 'setting--nag',
      disabled: !enabled,
    });

  keyboardInput = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Keyboard input',
      id: 'keyboard',
      checked: toggle(),
      change: toggle,
      disabled: !enabled,
    });

  private cmnToggle = (t: controls.ToggleSettings) => controls.toggle(t, this.trans, this.redraw);
}
