// no side effects allowed due to re-export by index.ts

import { h } from 'snabbdom';
import { type Toggle, myUserId, onClickAway } from '@/index';
import { addPointerListeners } from '@/pointer';
import * as licon from '@/licon';
import { type MaybeVNode, type MaybeVNodes, type VNode, dataIcon, onInsert } from './snabbdom';
import { cmnToggleWrap, cmnToggleWrapProp } from '@/view/controls';
import { pubsub } from '@/pubsub';

export const toggleButton = (toggle: Toggle, title: string): VNode =>
  h('button.fbt.board-menu-toggle-btn', {
    class: { active: toggle() },
    attrs: { title, 'data-icon': licon.Hamburger },
    hook: onInsert(el => addPointerListeners(el, { click: toggle.toggle })),
  });

export const boardMenu = (
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
  anonymous: boolean = !myUserId();

  constructor(readonly redraw: Redraw) {}

  flip = (name: string, active: boolean, onChange: () => void): VNode =>
    h(
      'button.button.text',
      {
        class: { active },
        attrs: { title: 'Hotkey: f', ...dataIcon(licon.ChasingArrows) },
        hook: onInsert(el => addPointerListeners(el, { click: onChange })),
      },
      name,
    );

  zenMode = (enabled = true): VNode =>
    cmnToggleWrap({
      id: 'zen',
      name: i18n.preferences.zenMode,
      checked: $('body').hasClass('zen'),
      change: () => pubsub.emit('zen'),
      disabled: !enabled,
      redraw: this.redraw,
    });

  voiceInput = (toggle: Toggle, enabled = true): VNode =>
    cmnToggleWrapProp({
      id: 'voice',
      name: i18n.preferences.inputMovesWithVoice,
      prop: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
      redraw: this.redraw,
    });

  keyboardInput = (toggle: Toggle, enabled = true): VNode =>
    cmnToggleWrapProp({
      id: 'keyboard',
      name: i18n.preferences.inputMovesWithTheKeyboard,
      prop: toggle,
      title: this.anonymous ? 'Must be logged in' : '',
      disabled: this.anonymous || !enabled,
      redraw: this.redraw,
    });

  blindfold = (toggle: Toggle, enabled = true): VNode =>
    cmnToggleWrapProp({
      id: 'blindfold',
      name: i18n.preferences.blindfold,
      prop: toggle,
      disabled: !enabled,
      redraw: this.redraw,
    });

  confirmMove = (toggle: Toggle, enabled = true): VNode =>
    cmnToggleWrapProp({
      id: 'confirmmove',
      name: i18n.preferences.moveConfirmation,
      prop: toggle,
      disabled: !enabled,
      redraw: this.redraw,
    });
}
