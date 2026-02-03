import * as licon from '../licon';
import { type VNode, hl, bind } from '@/view';
import type { Tab, VoiceChat } from './interfaces';
import discussionView from './discussion';
import { noteView } from './note';
import { moderationView } from './moderation';
import { type Hooks } from 'snabbdom';

import type { ChatCtrl } from './chatCtrl';

export function renderChat(ctrl: ChatCtrl, hook: Hooks = {}): VNode {
  return hl(
    'section.mchat' + (ctrl.isOptional ? '.mchat-optional' : ''),
    { class: { 'mchat-mod': !!ctrl.moderation }, hook },
    moderationView(ctrl.moderation) || normalView(ctrl),
  );
}

function renderVoiceChat(ctrl: ChatCtrl) {
  const p = ctrl.voiceChat;
  if (!p.enabled()) return;
  return p.instance
    ? p.instance.render()
    : hl('div.mchat__tab.voicechat.voicechat-slot', {
        attrs: { 'data-icon': licon.Handset, title: 'Voice chat' },
        hook: bind('click', () => {
          if (!p.loaded) {
            p.loaded = true;
            site.asset
              .loadEsm<VoiceChat>('bits.voiceChat', {
                init: { uid: ctrl.data.userId!, redraw: ctrl.redraw },
              })
              .then(m => {
                p.instance = m;
                ctrl.redraw();
              });
          }
        }),
      });
}

function normalView(ctrl: ChatCtrl) {
  const active = ctrl.getTab();
  return [
    hl('div.mchat__tabs.nb_' + ctrl.visibleTabs.length, { attrs: { role: 'tablist' } }, [
      ctrl.visibleTabs.map(t => renderTab(ctrl, t, active)),
      renderVoiceChat(ctrl),
    ]),
    hl(
      'div.mchat__content.' + active.key,
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
    'div.mchat__tab.' + tab.key,
    {
      attrs: { role: 'tab' },
      class: { 'mchat__tab-active': tab.key === active.key },
      hook: bind('click', e => {
        if ((e.target as HTMLElement).closest('input,label')) return;
        ctrl.setTab(tab);
        if (tab.key === 'discussion') ctrl.chatEnabled(true);
        ctrl.redraw();
      }),
    },
    tabName(ctrl, tab),
  );

function tabName(ctrl: ChatCtrl, tab: Tab) {
  if (tab.key === 'discussion') {
    const id = `chat-toggle-${ctrl.data.id}`;
    return [
      hl('span', ctrl.data.name),
      ctrl.isOptional &&
        hl('div.switch', [
          hl(`input#${id}.cmn-toggle`, {
            attrs: { type: 'checkbox', checked: ctrl.chatEnabled() },
            hook: bind('change', e => {
              ctrl.chatEnabled((e.target as HTMLInputElement).checked);
              ctrl.redraw();
            }),
          }),
          hl('label', {
            attrs: {
              for: id,
              title: i18n.site.toggleTheChat,
            },
          }),
        ]),
    ];
  }
  if (tab.key === 'note') return [hl('span', i18n.site.notes)];
  if (tab.key === ctrl.plugin?.key) return [hl('span', ctrl.plugin.name)];
  return [];
}
