import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { Tab } from './interfaces';
import discussionView from './discussion';
import { noteView } from './note';
import { moderationView } from './moderation';

import type * as palantir from 'palantir';
import ChatCtrl from './ctrl';

export default function (ctrl: ChatCtrl): VNode {
  return h(
    'section.mchat' + (ctrl.opts.alwaysEnabled ? '' : '.mchat-optional'),
    { class: { 'mchat-mod': !!ctrl.moderation }, hook: { destroy: ctrl.destroy } },
    moderationView(ctrl.moderation) || normalView(ctrl),
  );
}

function renderPalantir(ctrl: ChatCtrl) {
  const p = ctrl.palantir;
  if (!p.enabled()) return;
  return p.instance
    ? p.instance.render(h)
    : h('div.mchat__tab.palantir.palantir-slot', {
        attrs: { 'data-icon': licon.Handset, title: 'Voice chat' },
        hook: bind('click', () => {
          if (!p.loaded) {
            p.loaded = true;
            site.asset
              .loadEsm<palantir.Palantir>('palantir', {
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
  const active = ctrl.vm.tab;
  return [
    h('div.mchat__tabs.nb_' + ctrl.allTabs.length, { attrs: { role: 'tablist' } }, [
      ...ctrl.allTabs.map(t => renderTab(ctrl, t, active)),
      renderPalantir(ctrl),
    ]),
    h(
      'div.mchat__content.' + active,
      active === 'note' && ctrl.note
        ? [noteView(ctrl.note, ctrl.vm.autofocus)]
        : ctrl.plugin && active === ctrl.plugin.tab.key
        ? [ctrl.plugin.view()]
        : discussionView(ctrl),
    ),
  ];
}

const renderTab = (ctrl: ChatCtrl, tab: Tab, active: Tab) =>
  h(
    'div.mchat__tab.' + tab,
    {
      attrs: { role: 'tab' },
      class: { 'mchat__tab-active': tab === active },
      hook: bind('click', () => ctrl.setTab(tab)),
    },
    tabName(ctrl, tab),
  );

function tabName(ctrl: ChatCtrl, tab: Tab) {
  if (tab === 'discussion')
    return [
      h('span', ctrl.data.name),
      ctrl.opts.alwaysEnabled
        ? undefined
        : h('input', {
            attrs: { type: 'checkbox', title: ctrl.trans.noarg('toggleTheChat'), checked: ctrl.vm.enabled },
            hook: bind('change', (e: Event) => {
              ctrl.setEnabled((e.target as HTMLInputElement).checked);
            }),
          }),
    ];
  if (tab === 'note') return [h('span', ctrl.trans.noarg('notes'))];
  if (ctrl.plugin && tab === ctrl.plugin.tab.key) return [h('span', ctrl.plugin.tab.name)];
  return [];
}
