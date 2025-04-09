import * as licon from '../licon';
import { type VNode, looseH as h, bind } from '../snabbdom';
import type { Tab, Palantir } from './interfaces';
import discussionView from './discussion';
import { noteView } from './note';
import { moderationView } from './moderation';

import type { ChatCtrl } from './chatCtrl';

export function renderChat(ctrl: ChatCtrl): VNode {
  return h(
    'section.mchat' + (ctrl.isOptional ? '.mchat-optional' : ''),
    { class: { 'mchat-mod': !!ctrl.moderation } },
    moderationView(ctrl.moderation) || normalView(ctrl),
  );
}

function renderPalantir(ctrl: ChatCtrl) {
  const p = ctrl.palantir;
  if (!p.enabled()) return;
  return p.instance
    ? p.instance.render()
    : h('div.mchat__tab.palantir.palantir-slot', {
        attrs: { 'data-icon': licon.Handset, title: 'Voice chat' },
        hook: bind('click', () => {
          if (!p.loaded) {
            p.loaded = true;
            site.asset
              .loadEsm<Palantir>('palantir', {
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
    h('div.mchat__tabs.nb_' + ctrl.visibleTabs.length, { attrs: { role: 'tablist' } }, [
      ...ctrl.visibleTabs.map(t => renderTab(ctrl, t, active)),
      renderPalantir(ctrl),
    ]),
    h(
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
  h(
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
      h('span', ctrl.data.name),
      ctrl.isOptional &&
        h('div.switch', [
          h(`input#${id}.cmn-toggle.cmn-toggle--subtle`, {
            attrs: { type: 'checkbox', checked: ctrl.chatEnabled() },
            hook: bind('change', e => {
              ctrl.chatEnabled((e.target as HTMLInputElement).checked);
              ctrl.redraw();
            }),
          }),
          h('label', {
            attrs: {
              for: id,
              title: i18n.site.toggleTheChat,
            },
          }),
        ]),
    ];
  }
  if (tab.key === 'note') return [h('span', i18n.site.notes)];
  if (tab.key === ctrl.plugin?.key) return [h('span', ctrl.plugin.name)];
  return [];
}
