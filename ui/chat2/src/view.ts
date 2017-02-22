import { h } from 'snabbdom'
import { Ctrl, Tab } from './interfaces'
import discussionView from './discussion'
import { noteView } from './note'
import { moderationView } from './moderation'

export default function(ctrl: Ctrl) {

  return h('div#chat', {
    class: {
      side_box: true,
      mchat: true,
      mod: ctrl.opts.permissions.timeout
    }
  }, moderationView(ctrl.moderation) || normalView(ctrl))
}

function normalView(ctrl: Ctrl) {
  const active = ctrl.vm.tab;
  const tabs: Array<Tab> = ['discussion'];
  if (ctrl.note) tabs.push('note');
  return [
    h('div', {
      class: {
        chat_tabs: true,
        ['nb_' + tabs.length]: true
      }
    }, tabs.map(t => renderTab(ctrl, t, active))),
    h('div', {
      class: {
        content: true,
        [active]: true
      }
    }, (active === 'note' && ctrl.note) ? [noteView(ctrl.note)] : discussionView(ctrl))
  ]
}

function renderTab(ctrl: Ctrl, tab: Tab, active: Tab) {
  return h('div', {
    class: {
      tab: true,
      [tab]: true,
      active: tab === active
    },
    on: { click: [ctrl.setTab, tab] }
  }, tabName(ctrl, tab));
}

function tabName(ctrl: Ctrl, tab: Tab) {
  if (tab === 'discussion') return [
    h('span', ctrl.data.name),
    h('input.toggle_chat', {
      attrs: {
        type: 'checkbox',
        title: ctrl.trans('toggleTheChat'),
        checked: ctrl.vm.enabled
      },
      on: {
        change: (e: Event) => {
          ctrl.setEnabled((e.target as HTMLInputElement).checked);
        }
      }
    })
  ];
  if (tab === 'note') return ctrl.trans('notes');
}
