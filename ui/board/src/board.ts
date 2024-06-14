import { h } from 'snabbdom';
import { Toggle, onClickAway } from 'common/common';
import { bindMobileMousedown } from 'common/device';
import * as licon from 'common/licon';
import { MaybeVNode, MaybeVNodes, dataIcon, onInsert } from 'common/snabbdom';
import { Redraw } from 'chessground/types';
import * as controls from 'common/controls';

export const menu = (
  trans: Trans,
  redraw: Redraw,
  toggle: Toggle,
  content: (menu: BoardMenu) => MaybeVNodes,
): MaybeVNode =>
  toggle()
    ? h(
        'div.board-menu',
        { hook: onInsert(onClickAway(() => toggle(false))) },
        content(new BoardMenu(trans, redraw)),
      )
    : undefined;

export class BoardMenu {
  anonymous = document.querySelector('body[data-user]') === null;

  constructor(
    readonly trans: Trans,
    readonly redraw: Redraw,
  ) {}

  flip = (name: string, active: boolean, onChange: () => void) =>
    h(
      'button.button.text',
      {
        class: { active },
        attrs: { title: 'Hotkey: f', ...dataIcon(licon.ChasingArrows) },
        hook: onInsert(bindMobileMousedown(onChange)),
      },
      name,
    );

  zenMode = (enabled = true) =>
    this.cmnToggle({
      name: 'Zen mode',
      id: 'zen',
      checked: $('body').hasClass('zen'),
      change: () => site.pubsub.emit('zen'),
      disabled: !enabled,
    });

  voiceInput = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Voice input',
      id: 'voice',
      checked: toggle(),
      change: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
    });

  keyboardInput = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Keyboard input',
      id: 'keyboard',
      checked: toggle(),
      change: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
    });

  blindfold = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Blindfold',
      id: 'blindfold',
      checked: toggle(),
      change: toggle,
      disabled: !enabled,
    });
  confirmMove = (toggle: Toggle, enabled = true) =>
    this.cmnToggle({
      name: 'Confirm move',
      id: 'confirmmove',
      checked: toggle(),
      change: toggle,
      disabled: !enabled,
    });

  private cmnToggle = (t: controls.ToggleSettings) => controls.toggle(t, this.trans, this.redraw);
}
