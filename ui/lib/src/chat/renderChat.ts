import { type VNode, type Hooks, hl, bind, div, span } from '@/view';
import { cmnToggleProp } from '@/view/cmn-toggle';

import type { ChatCtrl } from './chatCtrl';
import discussionView from './discussion';
import type { Tab } from './interfaces';
import { moderationView } from './moderation';
import { noteView } from './note';

export function renderChat(ctrl: ChatCtrl, hook: Hooks = {}): VNode {
  return hl(
    'section.mchat' + (ctrl.isOptional ? '.mchat-optional' : ''),
    { class: { 'mchat-mod': !!ctrl.moderation }, hook },
    moderationView(ctrl.moderation) || normalView(ctrl),
  );
}

function normalView(ctrl: ChatCtrl) {
  const active = ctrl.getTab();
  return [
    div(`.mchat__tabs.nb_${ctrl.visibleTabs.length}`, { role: 'tablist' }, [
      ctrl.visibleTabs.map(t => renderTab(ctrl, t, active)),
    ]),
    div(
      `.mchat__content.${active.key}`,
      active.key === 'note' && ctrl.note
        ? [noteView(ctrl.note, ctrl.vm.autofocus)]
        : ctrl.plugin && active.key === ctrl.plugin.key
          ? [ctrl.plugin.view()]
          : discussionView(ctrl),
    ),
  ];
}

const renderTab = (ctrl: ChatCtrl, tab: Tab, active: Tab) =>
  hl(
    'button.mchat__tab.' + tab.key,
    {
      attrs: { role: 'tab' },
      class: { 'mchat__tab-active': tab.key === active.key },
      hook: bind('click', e => {
        if ((e.target as HTMLElement).closest('input,label')) return;
        ctrl.setTab(tab);
        ctrl.redraw();
      }),
    },
    tabName(ctrl, tab),
  );

function tabName(ctrl: ChatCtrl, tab: Tab) {
  if (tab.key === 'discussion') {
    const id = `chat-toggle-${ctrl.data.id}`;
    return [
      span(ctrl.data.name),
      ctrl.isOptional && cmnToggleProp({ id, prop: ctrl.chatEnabled, redraw: ctrl.redraw }),
    ];
  }
  if (tab.key === 'note') return [span(i18n.site.notes)];
  if (tab.key === ctrl.plugin?.key) return [span(ctrl.plugin.name)];
  return [];
}
