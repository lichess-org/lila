import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Ctrl, Tab } from './interfaces'
import discussionView from './discussion'
import { noteView } from './note'
import { moderationView } from './moderation'
import { bind } from './util'

export default function(ctrl: Ctrl): VNode {

  const mod = ctrl.moderation();

  console.log(ctrl);

  return h('div#chat.side_box.mchat' + (ctrl.opts.alwaysEnabled ? '' : '.optional'), {
    class: {
      mod: !!mod
    }
  }, moderationView(mod) || normalView(ctrl))
}

function normalView(ctrl: Ctrl) {
  const active = ctrl.vm.tab;
  const tabs: Array<Tab> = ['discussion'];
  if (ctrl.note) tabs.push('note');
  if (ctrl.plugin) tabs.push(ctrl.plugin.tab.key);
  return [
    h('div.chat_tabs.nb_' + tabs.length, tabs.map(t => renderTab(ctrl, t, active))),
    h('div.content.' + active,
      (active === 'note' && ctrl.note) ? [noteView(ctrl.note)] : (
        ctrl.plugin && active === ctrl.plugin.tab.key ? [ctrl.plugin.view()] : discussionView(ctrl)
      ))
  ]
}

function renderTab(ctrl: Ctrl, tab: Tab, active: Tab) {
  return h('div.tab.' + tab, {
    class: { active: tab === active },
    hook: bind('click', () => ctrl.setTab(tab))
  }, tabName(ctrl, tab));
}

function tabName(ctrl: Ctrl, tab: Tab) {
  if (tab === 'discussion') return [
    h('span', ctrl.data.name),
    ctrl.opts.alwaysEnabled ? undefined : h('input.toggle_chat', {
      attrs: {
        type: 'checkbox',
        title: ctrl.trans.noarg('toggleTheChat'),
        checked: ctrl.vm.enabled
      },
      hook: bind('change', (e: Event) => {
        ctrl.setEnabled((e.target as HTMLInputElement).checked);
      })
    })
  ];
  if (tab === 'note') return ctrl.trans.noarg('notes');
  if (ctrl.plugin && tab === ctrl.plugin.tab.key) return ctrl.plugin.tab.name;
}
