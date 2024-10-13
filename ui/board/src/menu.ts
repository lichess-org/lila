import { h, VNode } from 'snabbdom';
import { Toggle, onClickAway } from 'common/common';
import { bindMobileMousedown } from 'common/device';
import * as licon from 'common/licon';
import { MaybeVNode, MaybeVNodes, dataIcon, onInsert } from 'common/snabbdom';
import { Redraw } from 'chessground/types';
import * as controls from 'common/controls';
import { pubsub } from 'common/pubsub';

export const toggleButton = (toggle: Toggle, title: string): VNode =>
  h('button.fbt.board-menu-toggle', {
    class: { active: toggle() },
    attrs: { title, 'data-icon': licon.Hamburger },
    hook: onInsert(bindMobileMousedown(toggle.toggle)),
  });

export const menu = (
  redraw: Redraw,
  toggle: Toggle,
  content: (menu: BoardMenu) => MaybeVNodes,
): MaybeVNode =>
  toggle()
    ? h(
        'div.board-menu',
        { hook: onInsert(onClickAway(() => toggle(false))) },
        content(new BoardMenu(redraw)),
      )
    : undefined;

export class BoardMenu {
  anonymous: boolean = document.querySelector('body[data-user]') === null;

  constructor(readonly redraw: Redraw) {}

  flip = (name: string, active: boolean, onChange: () => void): VNode =>
    h(
      'button.button.text',
      {
        class: { active },
        attrs: { title: 'Hotkey: f', ...dataIcon(licon.ChasingArrows) },
        hook: onInsert(bindMobileMousedown(onChange)),
      },
      name,
    );

  zenMode = (enabled = true): VNode =>
    this.cmnToggle({
      name: 'Zen mode',
      id: 'zen',
      checked: $('body').hasClass('zen'),
      change: () => pubsub.emit('zen'),
      disabled: !enabled,
    });

  voiceInput = (toggle: Toggle, enabled = true): VNode =>
    this.cmnToggle({
      name: 'Voice input',
      id: 'voice',
      checked: toggle(),
      change: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
    });

  keyboardInput = (toggle: Toggle, enabled = true): VNode =>
    this.cmnToggle({
      name: 'Keyboard input',
      id: 'keyboard',
      checked: toggle(),
      change: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
    });

  blindfold = (toggle: Toggle, enabled = true): VNode =>
    this.cmnToggle({
      name: 'Blindfold',
      id: 'blindfold',
      checked: toggle(),
      change: toggle,
      disabled: !enabled,
    });

  confirmMove = (toggle: Toggle, enabled = true): VNode =>
    this.cmnToggle({
      name: 'Confirm move',
      id: 'confirmmove',
      checked: toggle(),
      change: toggle,
      disabled: !enabled,
    });

  private cmnToggle = (t: controls.ToggleSettings) => controls.toggle(t, this.redraw);
}
