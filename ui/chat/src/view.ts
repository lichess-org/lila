import { h, VNode } from 'snabbdom';
import { Ctrl, Tab } from './interfaces';
import discussionView from './discussion';
import { noteView } from './note';
import { moderationView } from './moderation';
import { bind } from './util';

export default function (ctrl: Ctrl): VNode {
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
    moderationView(mod) || normalView(ctrl)
  );
}

function renderPalantir(ctrl: Ctrl) {
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
            lichess.loadScript('javascripts/vendor/peerjs.min.js').then(() => {
              lichess.loadModule('palantir').then(() => {
                p.instance = window.Palantir!.palantir({
                  uid: ctrl.data.userId,
                  redraw: ctrl.redraw,
                });
                ctrl.redraw();
              });
            });
          }
        }),
      });
}

function normalView(ctrl: Ctrl) {
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
        : discussionView(ctrl)
    ),
  ];
}

function renderTab(ctrl: Ctrl, tab: Tab, active: Tab) {
  return h(
    'div.mchat__tab.' + tab,
    {
      class: { 'mchat__tab-active': tab === active },
      hook: bind('click', () => ctrl.setTab(tab)),
    },
    tabName(ctrl, tab)
  );
}

function tabName(ctrl: Ctrl, tab: Tab) {
  if (tab === 'discussion')
    return [
      h('span', ctrl.data.name),
      ctrl.opts.alwaysEnabled
        ? undefined
        : h('input', {
            attrs: {
              type: 'checkbox',
              title: ctrl.trans.noarg('toggleTheChat'),
              checked: ctrl.vm.enabled,
            },
            hook: bind('change', (e: Event) => {
              ctrl.setEnabled((e.target as HTMLInputElement).checked);
            }),
          }),
    ];
  if (tab === 'note') return [h('span', ctrl.trans.noarg('notes'))];
  if (ctrl.plugin && tab === ctrl.plugin.tab.key) return [h('span', ctrl.plugin.tab.name)];
  return [];
}
