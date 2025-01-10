import { loadCompiledScript } from 'common/assets';
import { bind } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import discussionView from './discussion';
import type { ChatCtrl, Tab } from './interfaces';
import { moderationView } from './moderation';
import { noteView } from './note';

export default function (ctrl: ChatCtrl): VNode {
  const mod = ctrl.moderation();

  return h(
    'section.mchat' + (ctrl.opts.alwaysEnabled ? '' : '.mchat-optional'),
    {
      class: {
        'mchat-mod': !!mod,
      },
      hook: {
        destroy: ctrl.destroy,
      },
    },
    moderationView(mod) || normalView(ctrl),
  );
}

function renderPalantir(ctrl: ChatCtrl) {
  const p = ctrl.palantir;
  if (!p.enabled()) return;
  return p.instance
    ? p.instance.render(h)
    : h('div.mchat__tab.palantir.palantir-slot', {
        attrs: {
          'data-icon': '',
          title: 'Voice chat',
        },
        hook: bind('click', () => {
          if (!p.loaded) {
            p.loaded = true;
            loadCompiledScript('palantir').then(() => {
              p.instance = window.lishogi.modules.palantir({
                uid: ctrl.data.userId,
                redraw: ctrl.redraw,
              });
              ctrl.redraw();
            });
          }
        }),
      });
}

function normalView(ctrl: ChatCtrl) {
  const active = ctrl.vm.tab;
  return [
    h('div.mchat__tabs.nb_' + ctrl.allTabs.length, [
      ...ctrl.allTabs.map(t => renderTab(ctrl, t, active)),
      renderPalantir(ctrl),
    ]),
    h(
      'div.mchat__content.' + active,
      active === 'note' && ctrl.note
        ? [noteView(ctrl.note)]
        : ctrl.plugin && active === ctrl.plugin.tab.key
          ? [ctrl.plugin.view()]
          : discussionView(ctrl),
    ),
  ];
}

function renderTab(ctrl: ChatCtrl, tab: Tab, active: Tab) {
  return h(
    'div.mchat__tab.' + tab,
    {
      class: { 'mchat__tab-active': tab === active },
      hook: bind('click', () => ctrl.setTab(tab)),
    },
    tabName(ctrl, tab),
  );
}

function tabName(ctrl: ChatCtrl, tab: Tab) {
  if (tab === 'discussion')
    return [
      h('span', ctrl.data.name),
      ctrl.opts.alwaysEnabled
        ? undefined
        : h('input', {
            attrs: {
              type: 'checkbox',
              title: i18n('toggleTheChat'),
              checked: ctrl.vm.enabled,
            },
            hook: bind('change', (e: Event) => {
              ctrl.setEnabled((e.target as HTMLInputElement).checked);
            }),
          }),
    ];
  if (tab === 'note') return [h('span', i18n('notes'))];
  if (ctrl.plugin && tab === ctrl.plugin.tab.key) return [h('span', ctrl.plugin.tab.name)];
  return [];
}
